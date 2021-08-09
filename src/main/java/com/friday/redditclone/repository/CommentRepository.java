package com.friday.redditclone.repository;

import com.friday.redditclone.models.Comment;
import com.friday.redditclone.models.Post;
import com.friday.redditclone.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost(Post post);

    List<Comment> findAllByUser(User user);
}
