package com.friday.redditclone.service;

import com.friday.redditclone.dto.VoteDto;
import com.friday.redditclone.exception.PostNotFoundException;
import com.friday.redditclone.exception.SpringRedditException;
import com.friday.redditclone.models.Post;
import com.friday.redditclone.models.User;
import com.friday.redditclone.models.Vote;
import com.friday.redditclone.models.VoteType;
import com.friday.redditclone.repository.PostRepository;
import com.friday.redditclone.repository.VoteRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final AuthService authService;

    public void vote(VoteDto voteDto) {
        Post post = postRepository.findById(voteDto.getPostId())
                .orElseThrow(() -> new PostNotFoundException(voteDto.getPostId().toString()));

        User user = authService.getCurrentUser();

        Optional<Vote> voteByPostAndUser =
                voteRepository.findTopByPostAndUserOrderByVoteIdDesc(
                        post, user
                );

        if (voteByPostAndUser.isPresent() && voteByPostAndUser.get().getVoteType().equals(voteDto.getVoteType())) {
            throw new SpringRedditException("You have already " + voteDto.getVoteType() + "'d this post");
        }

        if (VoteType.UPVOTE.equals(voteDto.getVoteType())) {
            post.setVoteCount(post.getVoteCount()+1);
        } else {
            post.setVoteCount(post.getVoteCount()-1);
        }

        voteRepository.save(mapVoteDtoToVote(voteDto, user, post));
        postRepository.save(post);


    }

    private Vote mapVoteDtoToVote(VoteDto voteDto, User user, Post post) {

        if (voteDto == null) { return null; }

        return Vote.builder()
                .voteType(voteDto.getVoteType())
                .post(post)
                .user(user)
                .build();
    }
}
