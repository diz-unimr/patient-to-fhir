package de.unimarburg.diz.patienttofhir.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.unimarburg.diz.patienttofhir.serializer.InstantDeserializer;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public class PatientModel implements Serializable {

    private int id;
    private char sex;
    private String invalidatedBy;
    private String patientId;
    private String firstName;
    private String lastName;
    private String title;
    private LocalDate birthDate;
    private Instant inserted;
    private Instant modified;
    private Instant deleted;

    public char getSex() {
        return sex;
    }

    @JsonSetter("sex_fk")
    public void setSex(char sex) {
        this.sex = sex;
    }

    public String getInvalidatedBy() {
        return invalidatedBy;
    }

    @JsonSetter("invalidated_by_pid")
    public void setInvalidatedBy(String invalidatedBy) {
        this.invalidatedBy = invalidatedBy;
    }

    public String getPatientId() {
        return patientId;
    }

    @JsonSetter("patient_id")
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getFirstName() {
        return firstName;
    }

    @JsonSetter("first_name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @JsonSetter("last_name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    @JsonSetter("birth_date")
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public boolean isDeleted() {
        return deleted != null;
    }

    public int getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(int id) {
        this.id = id;
    }

    public Instant getInserted() {
        return inserted;
    }

    @JsonSetter("inserted_when")
    @JsonDeserialize(using = InstantDeserializer.class)
    public void setInserted(Instant inserted) {
        this.inserted = inserted;
    }

    public Instant getModified() {
        return modified;
    }

    @JsonSetter("last_modified_when")
    @JsonDeserialize(using = InstantDeserializer.class)
    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public Instant getDeleted() {
        return deleted;
    }

    @JsonSetter("deleted_when")
    @JsonDeserialize(using = InstantDeserializer.class)
    public void setDeleted(Instant deleted) {
        this.deleted = deleted;
    }
}
