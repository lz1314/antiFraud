/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newland.anti.fraud.controller;

import com.newland.anti.fraud.model.Person;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/persons")
public class PersonController {

    private final Process<? extends Model> personsProcess;

    public PersonController(@Qualifier("persons") Process<? extends Model> personsProcess) {
        this.personsProcess = personsProcess;
    }

    @PostMapping
    public Map<String, Object> startProcess(@RequestBody PersonRequest request) {
        Map<String, Object> modelData = new HashMap<>();
        Person person = new Person(request.getName(), request.getAge());
        modelData.put("person", person);

        Model model = personsProcess.createModel();
        model.fromMap(modelData);

        ProcessInstance<? extends Model> processInstance = personsProcess.createInstance(model);
        processInstance.start();

        Map<String, Object> result = processInstance.variables().toMap();
        Map<String, Object> response = new HashMap<>();
        response.put("id", processInstance.id());
        response.put("person", result.get("person"));
        return response;
    }

    public static class PersonRequest {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
