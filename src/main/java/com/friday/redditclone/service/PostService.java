package com.friday.redditclone.service;

import com.friday.redditclone.dto.PostRequest;
import com.friday.redditclone.dto.PostResponse;
import com.friday.redditclone.exception.PostNotFoundException;
import com.friday.redditclone.exception.SubredditNotFoundException;
import com.friday.redditclone.models.*;
import com.friday.redditclone.repository.*;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final SubredditRepository subredditRepository;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse save(PostRequest postRequest) {
        Subreddit subreddit = subredditRepository.findByName(postRequest.getSubredditName())
                .orElseThrow(() -> new SubredditNotFoundException(postRequest.getSubredditName()));

        User currentUser = authService.getCurrentUser();

        Post post = postRepository.save(mapPostRequestToPost(postRequest, currentUser, subreddit));
        PostResponse postResponse = mapPostToPostResponse(post);
        return postResponse;
    }

    @Transactional
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id.toString()));
        return mapPostToPostResponse(post);
    }

    @Transactional
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::mapPostToPostResponse)
                .collect(toList());
    }

    @Transactional
    public List<PostResponse> getPostsBySubreddit(Long id) {
        Subreddit subreddit = subredditRepository.findById(id)
                .orElseThrow(SubredditNotFoundException::new);

        return postRepository.findAllBySubreddit(subreddit)
                .stream()
                .map(this::mapPostToPostResponse)
                .collect(toList());
    }

    @Transactional
    public List<PostResponse> getPostsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return postRepository.findByUser(user)
                .stream()
                .map(this::mapPostToPostResponse)
                .collect(toList());
    }


    @Transactional
    private Post mapPostRequestToPost(PostRequest postRequest, User user, Subreddit subreddit) {

        if (postRequest == null) { return null; }



        return Post.builder()
                .postId(postRequest.getPostId())
                .postName(postRequest.getPostName())
                .url(postRequest.getUrl())
                .description(postRequest.getDescription())
                .voteCount(0)
                .user(user)
                .created_date(Instant.now())
                .subreddit(subreddit)
                .build();

    }

    @Transactional
    private PostResponse mapPostToPostResponse(Post post) {
        if(post == null) { return null; }

        PostResponse postResponse = new PostResponse();

        postResponse.setId(post.getPostId());
        postResponse.setPostName(post.getPostName());
        postResponse.setUrl(post.getUrl());
        postResponse.setDescription(post.getDescription());
        postResponse.setUserName(post.getUser().getUsername());
        postResponse.setSubredditName(post.getSubreddit().getName());
        postResponse.setVoteCount(post.getVoteCount());
        postResponse.setCommentCount(commentRepository.findByPost(post).size());
        postResponse.setDuration(TimeAgo.using(post.getCreated_date().toEpochMilli()));
        postResponse.setUpVote(isPostUpVoted(post));
        postResponse.setDownVote(isPostDownVoted(post));

        return postResponse;
    }

    private boolean isPostUpVoted(Post post) {
        return checkVoteType(post, VoteType.UPVOTE);
    }

    private boolean isPostDownVoted(Post post) {
        return checkVoteType(post, VoteType.DOWNVOTE);
    }

    @Transactional
    private boolean checkVoteType(Post post, VoteType voteType) {

        if (authService.isLoggedIn()) {
            Optional<Vote> voteForPostByUser =
                    voteRepository.findTopByPostAndUserOrderByVoteIdDesc
                            (post, authService.getCurrentUser());

            return voteForPostByUser.filter(vote -> vote.getVoteType().equals(voteType)).isPresent();
        }
        return false;
    }


}
