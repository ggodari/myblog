package com.example.myboard.domain.comment.service;

import com.example.myboard.domain.comment.Comment;
import com.example.myboard.domain.comment.repository.CommentRepository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.stream.LongStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CommentServiceImplTest {

    @Autowired CommentService commentService;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    EntityManager em;

    private void clear() {
        em.flush();
        em.clear();
    }

    private Long saveComment() {
        Comment comment = Comment.builder().content("댓글").build();
        Long id = commentRepository.save(comment).getId();
        clear();
        return id;
    }

    private Long saveReComment(Long parentId) {
        Comment parent = commentRepository.findById(parentId).orElse(null);
        Comment comment = Comment.builder().content("댓글").parent(parent).build();

        Long id = commentRepository.save(comment).getId();
        clear();

        return id;
    }

    // 댓글을 삭제하는 경우
    // 대댓글이 남아 있는 경우
    // DB 와 화면에서는 지워지지 않고, "삭제된 댓글입니다" 라고 표시
    @Test
    public void 댓글삭제_대댓글이_남아있는_경우() throws Exception {
        // given
        Long commentId = saveComment();
        saveReComment(commentId);
        saveReComment(commentId);
        saveReComment(commentId);
        saveReComment(commentId);

        Assertions.assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(4);

        //when
        commentService.remove(commentId);
        clear();

        //then
        Comment findComment = commentService.findById(commentId);
        assertThat(findComment).isNotNull();
        assertThat(findComment.isRemoved()).isTrue();
        assertThat(findComment.getChildList().size()).isEqualTo(4);
    }

    // 댓글을 삭제하는 경우
    // 대댓글이 아예존재하지 않은 경우 : 곧바로 DB 에서 삭제
    @Test
    public void 댓글삭제_대댓글이_없는_경우() throws Exception {
        //given
        Long commentId = saveComment();

        //when
        commentService.remove(commentId);
        clear();

        //then
        Assertions.assertThat(commentService.findAll().size()).isEqualTo(0);
        assertThat(assertThrows(Exception.class, () -> commentService.findById(commentId)).getMessage()).isEqualTo("댓글이 없습니다.");
    }


    // 댓글을 삭제하는 경우
    // 대댓글이 존재하나 모두 삭제된 경우
    // 댓글과, 달려있는 대댓글 모두 DB에서 일괄 삭제, 화면상에도 표시되지 않음.

    @Test
    public void 댓글삭제_대댓글이_존재하나_모두_삭제된_대댓글인_경우() throws Exception {
        //given
        Long commentId = saveComment();
        Long reComment1Id = saveReComment(commentId);
        Long reComment2Id = saveReComment(commentId);
        Long reComment3Id = saveReComment(commentId);
        Long reComment4Id = saveReComment(commentId);

        Assertions.assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(4);
        clear();

        commentService.remove(reComment1Id);
        clear();

        commentService.remove(reComment2Id);
        clear();

        commentService.remove(reComment3Id);
        clear();

        commentService.remove(reComment4Id);
        clear();

        Assertions.assertThat(commentService.findById(reComment1Id).isRemoved()).isTrue();
        Assertions.assertThat(commentService.findById(reComment2Id).isRemoved()).isTrue();
        Assertions.assertThat(commentService.findById(reComment3Id).isRemoved()).isTrue();
        Assertions.assertThat(commentService.findById(reComment4Id).isRemoved()).isTrue();
        clear();

        //when
        commentService.remove(commentId);
        clear();

        //then
        LongStream.rangeClosed(commentId, reComment4Id).forEach(id ->
                assertThat(assertThrows(Exception.class, () -> commentService.findById(id)).getMessage()).isEqualTo("댓글이 없습니다."));
    }

    // 대댓글을 삭제하는 경우
    // 부모 댓글이 삭제되지 않은 경우
    // 내용만 삭제, DB에서는 삭제X
    @Test
    public void 대댓글삭제_부모댓글이_남아있는_경우() throws Exception {
        //given
        Long commentId = saveComment();
        Long reCommentId = saveReComment(commentId);

        //when
        commentService.remove(reCommentId);
        clear();

        //then
        Assertions.assertThat(commentService.findById(commentId)).isNotNull();
        Assertions.assertThat(commentService.findById(reCommentId)).isNotNull();
        Assertions.assertThat(commentService.findById(commentId).isRemoved()).isFalse();
        Assertions.assertThat(commentService.findById(reCommentId).isRemoved()).isTrue();
    }


    // 대댓글을 삭제하는 경우
    // 부모 댓글이 삭제되었고, 대댓글들고 모두 삭제된 경우
    // 부모를 포함한 모든 대댓글을 DB 에서 일괄 삭제, 화면상에서도 삭제
    @Test
    public void 대댓글삭제_부모댓글이_삭제된_경우_모든_대댓글이_삭제된_경우() throws Exception {
        //given
        Long commentId = saveComment();
        Long reComment1d = saveReComment(commentId);
        Long reComment2d = saveReComment(commentId);
        Long reComment3d = saveReComment(commentId);

        commentService.remove(reComment2d);
        clear();
        commentService.remove(commentId);
        clear();
        commentService.remove(reComment3d);
        clear();

        Assertions.assertThat(commentService.findById(commentId)).isNotNull();
        Assertions.assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(3);

        //when
        commentService.remove(reComment1d);
        clear();

        //then
        LongStream.rangeClosed(commentId, reComment3d).forEach(id ->
                assertThat(assertThrows(Exception.class, () -> commentService.findById(id)).getMessage()).isEqualTo("댓글이 없습니다.")
        );
    }

    // 대댓글을 삭제하는 경우
    // 부모 댓글이 삭제되어있고, 다른 대댓글이 아직 삭제되지 않고 남아 있는 경우
    // 해당 대댓글만 삭제, 그러나 DB 에서는 삭제되는 않고, 화면상에는 "삭제된 댓글입니다" 라고 표시

    @Test
    public void 대댓글삭제_부모댓글이_삭제된경우_다른_대댓글이_남아있는_경우() throws Exception {
        //given
        Long commentId = saveComment();
        Long reComment1d = saveReComment(commentId);
        Long reComment2d = saveReComment(commentId);
        Long reComment3d = saveReComment(commentId);

        commentService.remove(reComment3d);
        commentService.remove(commentId);
        clear();

        Assertions.assertThat(commentService.findById(commentId)).isNotNull();
        Assertions.assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(3);

        //when
        commentService.remove(reComment2d);
        Assertions.assertThat(commentService.findById(commentId)).isNotNull();

        //then
        Assertions.assertThat(commentService.findById(reComment2d)).isNotNull();
        Assertions.assertThat(commentService.findById(reComment2d).isRemoved()).isTrue();
        Assertions.assertThat(commentService.findById(reComment1d).getId()).isNotNull();
        Assertions.assertThat(commentService.findById(reComment3d).getId()).isNotNull();
        Assertions.assertThat(commentService.findById(commentId).getId()).isNotNull();
    }


}