package com.ground.domain.follow.service;

import com.ground.domain.follow.entity.Follow;
import com.ground.domain.notification.entity.NotificationAccount;
import com.ground.domain.notification.repository.NotificationAccountRepository;
import com.ground.domain.notification.repository.NotificationBoardRepository;
import com.ground.domain.user.entity.User;
import com.ground.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.qlrm.mapper.JpaResultMapper;

import com.ground.domain.follow.repository.FollowRepository;
import com.ground.domain.follow.dto.FollowDto;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final NotificationAccountRepository notificationAccountRepository;
    private final NotificationBoardRepository notificationBoardRepository;

    private final UserRepository userRepository;
    private final EntityManager em;

    // 팔로우
    @Transactional
    public void follow(Long fromUserId, Long toUserId) {
//        if(followRepository.findFollowByFromUserIdAndToUserId(fromUserId, toUserId) != null) throw new CustomApiException("이미 팔로우 하였습니다.");

        followRepository.follow(fromUserId, toUserId);

        User from = userRepository.findById(fromUserId).get();
        User to = userRepository.findById(toUserId).get();
        notificationAccountRepository.save(new NotificationAccount(from, to, false, LocalDateTime.now()));
    }

    // 팔로우 수락
    @Transactional
    public void followAccept(Long notiId, Long toUserId) {
//        if(followRepository.findFollowByFromUserIdAndToUserId(fromUserId, toUserId) != null) throw new CustomApiException("이미 팔로우 하였습니다.");
        NotificationAccount noti = notificationAccountRepository.findById(notiId).get();
        User from = noti.getFrom();
        User to = userRepository.findById(toUserId).get();

        Follow follow = followRepository.findByFromUserIdAndToUserId(from, to);
        follow.FollowAccept(true);

        noti.NotificationAccountDelete(true);

        notificationAccountRepository.save(new NotificationAccount(to, from, true, LocalDateTime.now()));
    }

    // 팔로우 거절
    @Transactional
    public void followDecline(Long notiId, Long toUserId) {
//        if(followRepository.findFollowByFromUserIdAndToUserId(fromUserId, toUserId) != null) throw new CustomApiException("이미 팔로우 하였습니다.");
        NotificationAccount noti = notificationAccountRepository.findById(notiId).get();
        User from = noti.getFrom();
        User to = userRepository.findById(toUserId).get();

        followRepository.unFollow(from.getId(), toUserId);
        noti.NotificationAccountDelete(true);
    }

    // 팔로우 삭제
    @Transactional
    public void unFollow(Long fromUserId, Long toUserId) {

        followRepository.unFollow(fromUserId, toUserId);
    }

    // 팔로워 목록 조회
    @Transactional
    public List<FollowDto> getFollower(long profileId, long userId) {
        StringBuffer sb = new StringBuffer();

        sb.append("SELECT u.id, u.username, u.nickname, u.user_image,");
        sb.append("if ((SELECT 1 FROM t_user_follow WHERE from_user_id = ? AND to_user_id = u.id), if ((SELECT 1 FROM t_user_follow WHERE from_user_id = ? AND to_user_id = u.id AND flag = TRUE), 1, 2), 0) AS followState, ");
        sb.append("if ((?=u.id), TRUE, FALSE) AS loginUser ");
        sb.append("FROM t_user u, t_user_follow f ");
        sb.append("WHERE u.id = f.from_user_id AND f.to_user_id = ? AND f.flag = TRUE");

        Query query = em.createNativeQuery(sb.toString())
                .setParameter(1, userId)
                .setParameter(2, userId)
                .setParameter(3, userId)
                .setParameter(4, profileId);

        JpaResultMapper result = new JpaResultMapper();
        List<FollowDto> followDtoList = result.list(query, FollowDto.class);
        return followDtoList;
    }

    // 팔로잉 목록 조회
    @Transactional
    public List<FollowDto> getFollowing(long profileId, long userId) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT u.id, u.username, u.nickname, u.user_image, ");
        sb.append("if ((SELECT 1 FROM t_user_follow WHERE from_user_id = ? AND to_user_id = u.id), if ((SELECT 1 FROM t_user_follow WHERE from_user_id = ? AND to_user_id = u.id AND flag = TRUE), 1, 2), 0) AS followState, ");
        sb.append("if ((?=u.id), TRUE, FALSE) AS loginUser ");
        sb.append("FROM t_user u, t_user_follow f ");
        sb.append("WHERE u.id = f.to_user_id AND f.from_user_id = ? AND f.flag = TRUE");

        Query query = em.createNativeQuery(sb.toString())
                .setParameter(1, userId)
                .setParameter(2, userId)
                .setParameter(3, userId)
                .setParameter(4, profileId);

        JpaResultMapper result = new JpaResultMapper();
        List<FollowDto> followDtoList = result.list(query, FollowDto.class);
        return followDtoList;
    }
}
