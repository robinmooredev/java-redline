package com.example.demo.dto;

public class ContractModification {
    
    public enum Action {
        INSERT, DELETE, MODIFY
    }
    
    private Action action;             // INSERT | DELETE | MODIFY
    private String sectionHeading;     // text of the heading that scopes the change (optional)
    private String oldParagraph;       // paragraph being replaced / deleted (optional for INSERT)
    private String newParagraph;       // paragraph being inserted / replacement text (optional for DELETE)
    private String comment;            // reviewer comment (optional)
    
    // Default constructor for Jackson
    public ContractModification() {
    }
    
    public ContractModification(Action action, String sectionHeading, String oldParagraph, 
                                String newParagraph, String comment) {
        this.action = action;
        this.sectionHeading = sectionHeading;
        this.oldParagraph = oldParagraph;
        this.newParagraph = newParagraph;
        this.comment = comment;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getSectionHeading() {
        return sectionHeading;
    }

    public void setSectionHeading(String sectionHeading) {
        this.sectionHeading = sectionHeading;
    }

    public String getOldParagraph() {
        return oldParagraph;
    }

    public void setOldParagraph(String oldParagraph) {
        this.oldParagraph = oldParagraph;
    }

    public String getNewParagraph() {
        return newParagraph;
    }

    public void setNewParagraph(String newParagraph) {
        this.newParagraph = newParagraph;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}