package com.example.myboard.domain.comment.repository;

import com.example.myboard.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {


}
