package com.chongcc.test.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Course {
    @Id
    private int id;
    private String name;
    private String category;
    private String description;
}
