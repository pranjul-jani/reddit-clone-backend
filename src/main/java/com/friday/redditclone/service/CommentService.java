package com.friday.redditclone.service;

import com.friday.redditclone.dto.CommentsDto;
import com.friday.redditclone.exception.PostNotFoundException;
import com.friday.redditclone.models.Comment;
import com.friday.redditclone.models.NotificationEmail;
import com.friday.redditclone.models.Post;
import com.friday.redditclone.models.User;
import com.friday.redditclone.repository.CommentRepository;
import com.friday.redditclone.repository.PostRepository;
import com.friday.redditclone.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final MailService mailService;
    private final MailContentBuilder mailContentBuilder;

    @Transactional
    public CommentsDto save(CommentsDto commentsDto) {

        User user = authService.getCurrentUser();
        Post post = postRepository.findById(commentsDto.getPostId())
                .orElseThrow(() -> new PostNotFoundException(commentsDto.getPostId().toString()));

        commentRepository.save(mapCommentsDtoToComment(commentsDto, user, post));

        String message = mailContentBuilder.build(authService.getCurrentUser().getUsername() +
                "posted a comment on your post " +
                post.getUrl()
        );

        sendCommentNotification(message, post.getUser());
        return commentsDto;
    }

    public List<CommentsDto> getAllCommentsForPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId.toString()));

        return commentRepository.findByPost(post)
                .stream()
                .map(this::mapCommentToCommentsDto)
                .collect(toList());
    }


    public List<CommentsDto> getAllCommentsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return commentRepository.findAllByUser(user)
                .stream()
                .map(this::mapCommentToCommentsDto)
                .collect(toList());
    }

    @Transactional
    private Comment mapCommentsDtoToComment(CommentsDto commentsDto, User user, Post post) {
        if (commentsDto == null) { return null; }

        return Comment.builder()
                .user(user)
                .post(post)
                .text(commentsDto.getText())
                .createdDate(Instant.now())
                .build();

    }

    @Transactional
    private CommentsDto mapCommentToCommentsDto(Comment comment) {
        if (comment == null) { return null; }

        CommentsDto commentsDto = new CommentsDto();

        commentsDto.setId(comment.getId());
        commentsDto.setPostId(comment.getPost().getPostId());
        commentsDto.setText(comment.getText());
        commentsDto.setUserName(comment.getUser().getUsername());
        commentsDto.setCreatedDate(comment.getCreatedDate());

        return commentsDto;
    }

    @Transactional
    private void sendCommentNotification(String message, User user) {
        NotificationEmail notificationEmail = new NotificationEmail();
        notificationEmail.setRecipient(user.getEmail());
        notificationEmail.setSubject("New Comment on your post");
        notificationEmail.setBody(message);

        mailService.sendMail(notificationEmail);
    }
}
