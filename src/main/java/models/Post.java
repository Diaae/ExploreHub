package models;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("ALL")
@NamedQueries({
        @NamedQuery(name="Post.getPostById", query = "Select p from Post p WHERE p.Id = :pid"),
        @NamedQuery(name="Post.getPostbyThread", query = "SELECT p FROM Post p WHERE p.topic = :t"),
        @NamedQuery(name="Post.getLastPost", query = "SELECT p FROM Post p WHERE p.topic.category = :f ORDER BY p.postLastEdited DESC")
})

@Entity
@Table(name="post")
public class Post {
    public Post(){

    }

    public Post(Account postAuthor, String postContent, String postTime){
        this.postAuthor = postAuthor;
        this.postContent = postContent.getBytes();
        this.postTime = postTime;
        this.postLastEdited = postTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int Id;

    @ManyToOne
    @JoinColumn(name = "threadID", nullable=false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postAuthor", nullable=false)
    private Account postAuthor;

    @Column(length=20)
    @Basic(optional = false)
    private byte[] postContent;

    @Column(length=20)
    @Basic(optional = false)
    private String postTime;

    public int getPostID() {
        return Id;
    }

    public void setPostID(int postID) {
        this.Id = postID;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Account getAuthor() {
        return postAuthor;
    }

    public void setUser(Account author) {
        this.postAuthor = author;
    }

    public String getPostContent() {  return new String(postContent, StandardCharsets.UTF_8);
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent.getBytes();
    }

    public String getPostTime() {
        return postTime;
    }

    public void setPostTime(String postTime) {
        this.postTime = postTime;
    }

    public String getPostLastEdited() {
        return postLastEdited;
    }

    public void setPostLastEdited(String postLastEdited) {
        this.postLastEdited = postLastEdited;
    }

    @Basic(optional = false)
    private String postLastEdited;
}
