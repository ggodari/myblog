package com.example.myboard.domain.comment;

import com.example.myboard.domain.member.Member;
import com.example.myboard.domain.post.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "COMMENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private Member writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Lob
    @Column(nullable = false)
    private String content;

    private boolean isRemoved = false;

    //== 부모 댓글을 삭제해도 자식 댓글은 남아 있음 ==//
    @OneToMany(mappedBy = "parent")
    private List<Comment> childList = new ArrayList<>();



    //== 연관관계 편의 메서드 ==//
    public void confirmWriter(Member writer) {
        this.writer = writer;
        writer.addComment(this);
    }

    public void confirmPost(Post post) {
        this.post = post;
        post.addComment(this);
    }

    public void confirmParent(Comment parent) {
        this.parent = parent;
        parent.addChild(this);
    }

    public void addChild(Comment child) {
        childList.add(child);
    }



    //== 수정 ==//
    public void updateContent(String content) {
        this.content = content;
    }

    //== 삭제 ==//
    public void remove() {
        this.isRemoved = true;
    }


    @Builder
    public Comment(Member writer, Post post, Comment parent, String content) {
        this.writer = writer;
        this.post = post;
        this.parent = parent;
        this.content = content;
    }

    //== 비지니스 로직 ==//
    public List<Comment> findRemoveableList() {
        List<Comment> result = new ArrayList<>();


        // 대댓글인 경우 부모가 삭제되었고, 모든 댓글이 삭제된 경우 리스트를 반환
        Optional.ofNullable(this.parent).ifPresent(
                parentComment -> {  //대댓글인 경우
                    if(parentComment.isRemoved() && parentComment.isAllChildRemoved()) {
                        result.addAll(parentComment.getChildList());
                        result.add(parentComment);
                    }
                }
        );

        // 댓글인 경우 모든 대댓글이 삭제되었다면 리스트를 반환
        if (! Optional.ofNullable(this.parent).isPresent() ) {
            if (isAllChildRemoved()) {
                result.add(this);
                result.addAll(this.getChildList());
            }
        }

        return result;
    }

    // 모든 자식 댓글이 삭제되었는지 판단
    private boolean isAllChildRemoved() {
        return getChildList().stream().
                map(Comment::isRemoved)             // 지워졌는지 여부로 판단
                .filter(isRemoved -> !isRemoved)    // 지워졌으면 true, 안지워졌으면 false
                .findAny()                          // 지워지지 않은게 하나라도 있다면 false 를 리턴
                .orElse(false);
    }
}
