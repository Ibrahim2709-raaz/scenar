package com.example.scenar.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared data contract between the victim flow, AI pipeline and responder dashboard.
 * Unknown values should remain null or be added to missingInformation; they must not
 * be silently guessed by an AI implementation.
 */
public class IncidentReport implements Serializable {
    private String incidentType;
    private String summary;
    private Integer peopleAffected;
    private List<String> criticalConditions = new ArrayList<>();
    private List<String> hazards = new ArrayList<>();
    private String locationDescription;
    private Double latitude;
    private Double longitude;
    private List<String> medicalFlags = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private List<String> uncertainties = new ArrayList<>();

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getPeopleAffected() {
        return peopleAffected;
    }

    public void setPeopleAffected(Integer peopleAffected) {
        this.peopleAffected = peopleAffected;
    }

    public List<String> getCriticalConditions() {
        return criticalConditions;
    }

    public void setCriticalConditions(List<String> criticalConditions) {
        this.criticalConditions = nonNullList(criticalConditions);
    }

    public List<String> getHazards() {
        return hazards;
    }

    public void setHazards(List<String> hazards) {
        this.hazards = nonNullList(hazards);
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<String> getMedicalFlags() {
        return medicalFlags;
    }

    public void setMedicalFlags(List<String> medicalFlags) {
        this.medicalFlags = nonNullList(medicalFlags);
    }

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(List<String> missingInformation) {
        this.missingInformation = nonNullList(missingInformation);
    }

    public List<String> getUncertainties() {
        return uncertainties;
    }

    public void setUncertainties(List<String> uncertainties) {
        this.uncertainties = nonNullList(uncertainties);
    }

    private static List<String> nonNullList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
