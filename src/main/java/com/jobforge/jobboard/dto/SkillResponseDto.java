package com.jobforge.jobboard.dto;

import lombok.Data;

@Data
public class SkillResponseDto {
    private Long id;
    private String name;

    // NO jobResponseDto inside here! Sending the skill with the jobs just like
    // it is defined on the Entity is problematic and will cause an infinite loop of serialization
    // on the frontEnd! This is because OF THE MANY-TO-MANY relationship Between skills and jobs.
    // A job has many skills, which have many jobs and so on...
}