package com.example.decision.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result from rule engine evaluation.
 */
public class RuleResult {
    private DecisionOutcome suggestedOutcome;
    private final List<String> firedRules = new ArrayList<>();
    private final List<String> reasonCodes = new ArrayList<>();
    private String ruleVersion;
    
    public void addFiredRule(String ruleName) {
        firedRules.add(ruleName);
    }
    
    public void addReasonCode(String code) {
        reasonCodes.add(code);
    }
    
    public DecisionOutcome getSuggestedOutcome() {
        return suggestedOutcome;
    }
    
    public void setSuggestedOutcome(DecisionOutcome outcome) {
        this.suggestedOutcome = outcome;
    }
    
    public List<String> getFiredRules() {
        return new ArrayList<>(firedRules);
    }
    
    public List<String> getReasonCodes() {
        return new ArrayList<>(reasonCodes);
    }
    
    public String getRuleVersion() {
        return ruleVersion;
    }
    
    public void setRuleVersion(String version) {
        this.ruleVersion = version;
    }
}
