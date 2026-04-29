package com.chongcc.test.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "ai_dial_hst")
public class DialHst {
    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "dial_type", length = 20)
    private String dialType;

    @Column(name = "conversation_id", length = 20, columnDefinition = "CHAR(20)")
    private String conversationId;

    @Column(name = "chat_id", length = 20, columnDefinition = "CHAR(20)")
    private String chatId;

    @Column(name = "question", length = 100)
    private String question;

    @Column(name = "user_id")
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
}
