/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appium.uiautomator2.model.api;

import com.google.gson.annotations.SerializedName;
import io.appium.uiautomator2.model.RequiredField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.appium.uiautomator2.utils.StringHelpers.isBlank;

public abstract class BaseModel {
    private List<Field> getProperties() {
        List<Field> result = new ArrayList<>();
        Collections.addAll(result, getClass().getDeclaredFields());
        Class<?> superclass = getClass().getSuperclass();
        while (superclass != null && superclass != BaseModel.class) {
            Collections.addAll(result, superclass.getDeclaredFields());
            superclass = superclass.getSuperclass();
        }
        return result;
    }

    public BaseModel validate() {
        for (Field f : getProperties()) {
            String jsonFieldName = f.getName();
            SerializedName serializedNameField = f.getAnnotation(SerializedName.class);
            if (serializedNameField != null && !isBlank(serializedNameField.value())) {
                jsonFieldName = serializedNameField.value();
            }

            Object fieldValue;
            try {
                f.setAccessible(true);
                fieldValue = f.get(this);
                if (fieldValue == null && f.getAnnotation(RequiredField.class) != null) {
                    throw new IllegalArgumentException(
                            String.format("%s: The mandatory field '%s' is not present in JSON",
                                    getClass().getSimpleName(), jsonFieldName));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }

            if (fieldValue == null) {
                continue;
            }

            Class<?> fieldType = f.getType();
            if (BaseModel.class.isAssignableFrom(fieldType)) {
                ((BaseModel) fieldValue).validate();
            }
            if (List.class.isAssignableFrom(fieldType)) {
                //noinspection rawtypes
                for (Object item : (List) fieldValue) {
                    if (item instanceof BaseModel) {
                        ((BaseModel) item).validate();
                    }
                }
            }
        }
        return this;
    }
}
