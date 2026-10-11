package com.campusconnect.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "print_files")
public class PrintFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    public Long id;
    @JsonIgnore
    @ManyToOne(optional = false) @JoinColumn(name = "print_job_id")
    public PrintJob job;
    @Column(nullable = false)
    public String originalName;
    @JsonIgnore
    @Column(nullable = false)
    public String storedName;
    public String contentType;
    public int pages;
    public long sizeBytes;
    public boolean removed = false;
}
