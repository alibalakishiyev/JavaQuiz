package com.ali.quizutility;

public class MathQuiz {
    private String text;
    private Object correctAnswer;  // Burada 'Object' istifadə edirik

    // Konstruktor
    public MathQuiz(String text, Object correctAnswer) {
        this.text = text;
        this.correctAnswer = correctAnswer;
    }

    // Getter-lar
    public String getText() {
        return text;
    }

    public Object getCorrectAnswer() {
        return correctAnswer;
    }
}


