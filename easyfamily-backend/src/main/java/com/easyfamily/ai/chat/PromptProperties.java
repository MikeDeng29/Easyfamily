package com.easyfamily.ai.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Prompt templates loaded from prompts.yml (easyfamily.ai.prompts.*).
 * Edit prompts.yml to change AI behaviour without touching Java code.
 */
@ConfigurationProperties(prefix = "easyfamily.ai.prompts")
public class PromptProperties {

    private String introDefault;
    private String introCustom;
    private String capabilities;
    private String moduleVehicle;
    private String moduleBill;
    private String moduleFamily;
    private String moduleMemory;
    private String toneFooter;
    private String personaWarm;
    private String personaStrict;
    private String personaHumorous;

    public String getIntroDefault() { return introDefault; }
    public void setIntroDefault(String v) { this.introDefault = v; }

    public String getIntroCustom() { return introCustom; }
    public void setIntroCustom(String v) { this.introCustom = v; }

    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String v) { this.capabilities = v; }

    public String getModuleVehicle() { return moduleVehicle; }
    public void setModuleVehicle(String v) { this.moduleVehicle = v; }

    public String getModuleBill() { return moduleBill; }
    public void setModuleBill(String v) { this.moduleBill = v; }

    public String getModuleFamily() { return moduleFamily; }
    public void setModuleFamily(String v) { this.moduleFamily = v; }

    public String getModuleMemory() { return moduleMemory; }
    public void setModuleMemory(String v) { this.moduleMemory = v; }

    public String getToneFooter() { return toneFooter; }
    public void setToneFooter(String v) { this.toneFooter = v; }

    public String getPersonaWarm() { return personaWarm; }
    public void setPersonaWarm(String v) { this.personaWarm = v; }

    public String getPersonaStrict() { return personaStrict; }
    public void setPersonaStrict(String v) { this.personaStrict = v; }

    public String getPersonaHumorous() { return personaHumorous; }
    public void setPersonaHumorous(String v) { this.personaHumorous = v; }
}
