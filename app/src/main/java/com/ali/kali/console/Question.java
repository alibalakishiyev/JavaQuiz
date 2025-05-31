package com.ali.kali.console;

public class Question {
    private final String question;
    private final String answer;
    private final String description;

    public Question(String question, String answer, String description) {
        this.question = question;
        this.answer = answer;
        this.description = description;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getDescription() {
        return description;
    }
}
