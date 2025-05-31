package com.ali.kali.comandlist;

public class LinuxCommands {

    private String command;
    private String description;
    private String example;
    private String details;

    public LinuxCommands() {}

    public LinuxCommands(String command, String description, String example, String details) {
        this.command = command;
        this.description = description;
        this.example = example;
        this.details = details;
    }

    // getter və setterlər
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
