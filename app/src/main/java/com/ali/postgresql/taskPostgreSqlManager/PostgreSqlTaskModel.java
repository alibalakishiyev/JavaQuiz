package com.ali.postgresql.taskPostgreSqlManager;

import java.util.List;

public class PostgreSqlTaskModel {
    private int id;
    private String title;
    private String description;
    private String initialQuery;
    private List<Test> tests;
    private String solution;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInitialQuery() { return initialQuery; }
    public void setInitialQuery(String initialQuery) { this.initialQuery = initialQuery; }

    public List<Test> getTests() { return tests; }
    public void setTests(List<Test> tests) { this.tests = tests; }

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }

    public static class Test {
        private String input;
        private String expected;
        private String description;

        // Getters and Setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }

        public String getExpected() { return expected; }
        public void setExpected(String expected) { this.expected = expected; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}