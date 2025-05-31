package com.ali.kali.comandlist;

import java.util.List;

public class LinuxCommandsWrapper {

    private List<CommandCategory> linux_commands;

    public LinuxCommandsWrapper(List<CommandCategory> linux_commands) {
        this.linux_commands = linux_commands;
    }

    public List<CommandCategory> getLinux_commands() {
        return linux_commands;
    }
}
