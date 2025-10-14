package com.jobforge.jobboard;

import java.text.Normalizer;

public class SkillNormalizer {

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }

        // Trim spaces
        String skill = input.trim().toLowerCase();

        // Normalize unicode (NFKD form separates diacritics)
        skill = Normalizer.normalize(skill, Normalizer.Form.NFKD);

        // Remove diacritics
        skill = skill.replaceAll("\\p{M}", "");

        // Collapse multiple spaces to single
        skill = skill.replaceAll("\\s+", " ");

        // Replace common punctuation variants with spaces
        skill = skill.replaceAll("[-_]", " ");

        return skill;
    }

}