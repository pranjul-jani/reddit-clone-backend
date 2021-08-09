package com.friday.redditclone.service;

import com.friday.redditclone.dto.SubredditDto;
import com.friday.redditclone.exception.SpringRedditException;
import com.friday.redditclone.models.Subreddit;
import com.friday.redditclone.repository.SubredditRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
@Slf4j
public class SubredditService {

    private final SubredditRepository subredditRepository;

    @Transactional
    public SubredditDto save(SubredditDto subredditDto) {

       Subreddit subreddit = subredditRepository.save(mapSubredditDtoToSubreddit(subredditDto));
       subredditDto.setId(subreddit.getId());
       return subredditDto;
    }

    @Transactional()
    public List<SubredditDto> getAll() {

        return subredditRepository.findAll()
                .stream()
                .map(this::mapSubredditToSubredditDto)
                .collect(toList());
    }

    private SubredditDto mapSubredditToSubredditDto(Subreddit subreddit) {

        return SubredditDto.builder()
                .id(subreddit.getId())
                .name(subreddit.getName())
                .description(subreddit.getDescription())
                .numberOfPosts(subreddit.getPosts().size())
                .build();


    }

    private Subreddit mapSubredditDtoToSubreddit(SubredditDto subredditDto) {
        return Subreddit.builder()
                .name(subredditDto.getName())
                .description(subredditDto.getDescription())
                .build();
    }


    public SubredditDto getSubreddit(Long id) {

        Subreddit subreddit = subredditRepository.findById(id)
                .orElseThrow(() -> new SpringRedditException("No subreddit found with the following id"));

        return mapSubredditToSubredditDto(subreddit);
    }
}
