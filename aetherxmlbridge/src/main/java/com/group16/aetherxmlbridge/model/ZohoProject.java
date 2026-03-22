// define zoho project object

package com.group16.aetherxmlbridge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZohoProject {
    private String id;
    private String dealName;
    private String stage;
    private String companyWebsite;
    private String companyContext;
    private String productPurpose;
    private String productionNotes;
    private String customerConcerns;
}
