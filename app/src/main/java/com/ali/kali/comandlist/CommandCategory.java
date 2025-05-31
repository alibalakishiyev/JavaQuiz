package com.ali.kali.comandlist;

import java.util.List;

public class CommandCategory {

    private String category;
    private List<LinuxCommands> commands;

    public CommandCategory() {}

    public CommandCategory(String category, List<LinuxCommands> commands) {
        this.category = category;
        this.commands = commands;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<LinuxCommands> getCommands() { return commands; }
    public void setCommands(List<LinuxCommands> commands) { this.commands = commands; }
}
