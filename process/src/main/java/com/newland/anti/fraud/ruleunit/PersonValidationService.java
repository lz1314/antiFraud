package com.newland.anti.fraud.ruleunit;

import com.newland.anti.fraud.model.Person;
import org.drools.ruleunits.api.DataSource;
import org.drools.ruleunits.api.DataStore;
import org.drools.ruleunits.api.RuleUnitData;

public class PersonValidationService implements RuleUnitData {
    private final DataStore<Person> persons = DataSource.createStore();

    public DataStore<Person> getPersons() {
        return persons;
    }
}
