package com.stackoverflow.dto;

import java.time.*;
import java.util.*;

import javax.validation.constraints.*;

/**
 * Auto-generated by {@link cool.klass.generator.dto.DataTransferObjectsGenerator}
 */
public class QuestionDTO
{
    private Long id;
    private Instant system;
    private Instant systemFrom;
    private Instant systemTo;
    @NotNull
    private String createdById;
    @NotNull
    private Instant createdOn;
    @NotNull
    private String lastUpdatedById;
    @NotNull
    private String body;
    @NotNull
    private String title;
    @NotNull
    private StatusDTO status;
    @NotNull
    private Boolean deleted;

    private List<AnswerDTO> answers;
    private List<QuestionVoteDTO> votes;
    private List<QuestionTagMappingDTO> tags;
    private QuestionVersionDTO version;
    private UserDTO createdBy;
    private UserDTO lastUpdatedBy;

    public Long getId()
    {
        return this.id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Instant getSystem()
    {
        return this.system;
    }

    public void setSystem(Instant system)
    {
        this.system = system;
    }

    public Instant getSystemFrom()
    {
        return this.systemFrom;
    }

    public void setSystemFrom(Instant systemFrom)
    {
        this.systemFrom = systemFrom;
    }

    public Instant getSystemTo()
    {
        return this.systemTo;
    }

    public void setSystemTo(Instant systemTo)
    {
        this.systemTo = systemTo;
    }

    public String getCreatedById()
    {
        return this.createdById;
    }

    public void setCreatedById(String createdById)
    {
        this.createdById = createdById;
    }

    public Instant getCreatedOn()
    {
        return this.createdOn;
    }

    public void setCreatedOn(Instant createdOn)
    {
        this.createdOn = createdOn;
    }

    public String getLastUpdatedById()
    {
        return this.lastUpdatedById;
    }

    public void setLastUpdatedById(String lastUpdatedById)
    {
        this.lastUpdatedById = lastUpdatedById;
    }

    public String getBody()
    {
        return this.body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public String getTitle()
    {
        return this.title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public StatusDTO getStatus()
    {
        return this.status;
    }

    public void setStatus(StatusDTO status)
    {
        this.status = status;
    }

    public Boolean getDeleted()
    {
        return this.deleted;
    }

    public void setDeleted(Boolean deleted)
    {
        this.deleted = deleted;
    }

    public List<AnswerDTO> getAnswers()
    {
        return this.answers;
    }

    public void setAnswers(List<AnswerDTO> answers)
    {
        this.answers = answers;
    }

    public List<QuestionVoteDTO> getVotes()
    {
        return this.votes;
    }

    public void setVotes(List<QuestionVoteDTO> votes)
    {
        this.votes = votes;
    }

    public List<QuestionTagMappingDTO> getTags()
    {
        return this.tags;
    }

    public void setTags(List<QuestionTagMappingDTO> tags)
    {
        this.tags = tags;
    }

    public QuestionVersionDTO getVersion()
    {
        return this.version;
    }

    public void setVersion(QuestionVersionDTO version)
    {
        this.version = version;
    }

    public UserDTO getCreatedBy()
    {
        return this.createdBy;
    }

    public void setCreatedBy(UserDTO createdBy)
    {
        this.createdBy = createdBy;
    }

    public UserDTO getLastUpdatedBy()
    {
        return this.lastUpdatedBy;
    }

    public void setLastUpdatedBy(UserDTO lastUpdatedBy)
    {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
