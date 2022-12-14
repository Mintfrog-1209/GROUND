package com.ground.domain.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import javax.transaction.Transactional;

import com.ground.domain.board.entity.Board;
import com.ground.domain.board.repository.BoardRepository;
import com.ground.domain.follow.entity.Follow;
import com.ground.domain.follow.repository.FollowRepository;
import com.ground.domain.global.entity.Category;
import com.ground.domain.global.repository.CategoryRepository;
import com.ground.domain.jwt.JwtTokenProvider;

import com.ground.domain.user.dto.*;
import com.ground.domain.user.entity.UserCategory;
import com.ground.domain.user.repository.UserCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ground.domain.user.entity.User;
import com.ground.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import springfox.documentation.schema.Entry;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {
	
	@Autowired 
	private final UserRepository userRepository;

	private final CategoryRepository categoryRepository;
	private final UserCategoryRepository userCategoryRepository;
	private final BoardRepository boardRepository;
	@Autowired
	private final FollowRepository followRepository;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	
	@Transactional
	//회원가입
	public boolean saveUser(UserRegisterDto params) {
		try {
			userRepository.save(params.toEntity());
			return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	@Transactional
	//아이디 중복 확인
	public boolean checkUsername(String username) {
		Optional<User> result = userRepository.findByUsername(username);
		if(result.isEmpty()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	@Transactional
	//닉네임 중복 확인
	public boolean checkNickname(String nickname) {
		Optional<User> result = userRepository.findByNickname(nickname);
		if(result.isEmpty()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	@Transactional
	//이메일 중복 확인
	public boolean checkEmail(String email) {
		Optional<User> result = userRepository.findByEmail(email);
		if(result.isEmpty()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	@Transactional
	//유저 계정 삭제 처리
	public boolean deleteUser(String username) {
		User user = userRepository.findByUsername(username).orElseThrow();
		try {
			user.deleteUser();
			return true;
		}
		catch(Exception e){
			return false;
		}
		
	}
	
	@Transactional
	//아이디 찾기
	public String findId(String email) {
		User user = userRepository.findByEmail(email).orElseThrow();
		return user.getUsername();
	}
	
	public boolean modifyPassCheck(UserFindPassDto params) {
		Optional<User> result = userRepository.findByEmailAndUsername(params.getEmail(), params.getUsername());
		if(result.isEmpty()) {
			return false;
		}
		else {
			return true;
		}
	}
	
	@Transactional
	//비밀번호 변경
	public boolean modifyPass(User user, UserModifyPassDto params) {
		try {
			user.modifyPass(params.getPass());
			user.saveModDttm(LocalDateTime.now());
			return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	//토큰 생성 후 저장 로그인
	@Transactional
	public UserLoginResponseDto login(UserLoginDto params) {
		UserLoginResponseDto ulrd = new UserLoginResponseDto();
		try {
			Optional<User> user = userRepository.findByUsernameAndPass(params.getUsername(), params.getPass());
			if(user.isEmpty()) {
				ulrd.setResult("fail");
				return ulrd;
			}
			else {
				String ftoken = jwtTokenProvider.createToken(user.get().getUsername());
				user.get().saveFtoken(ftoken);
				ulrd.setResult("success");
				ulrd.setFtoken(ftoken);
				ulrd.setRegisterYN(user.get().isRegisterYN());
				return ulrd;
			}
		}
		catch(Exception e) {
			ulrd.setResult("fail");
			return ulrd;
		}
	}
	
	public UserStateDto userState(String ftoken) {
		String username = jwtTokenProvider.getSubject(ftoken);
		
		User user = userRepository.findByUsername(username).orElseThrow(IllegalArgumentException::new);
		UserStateDto userstate = new UserStateDto(user.getId(), user.getUsername(), user.getEmail(), user.getNickname(), 
	    		user.getFtoken(), user.getIntroduce(), user.getUserImage(), user.getGender(), user.getAge(), 
	    		user.isPrivateYN(), user.isRegisterYN());
		return userstate;
	}
	
	
	@Transactional
	public boolean logoutUser(String ftoken) {
		String username = jwtTokenProvider.getSubject(ftoken);
		Optional<User> user = userRepository.findByUsername(username);
		user.get().saveFtoken(null);
		return true;
	}


	// -----------------BSH-----------------
	// 프로필 조회
	@Transactional
    public UserProfileDto getUserProfile(Long id, Long loginUserId) {
		UserProfileDto userProfileDto = new UserProfileDto();
		int follow = 0;
		if (id == loginUserId) { follow = 1; }
		else {
			User from = userRepository.findById(loginUserId).get();
			User to = userRepository.findById(id).get();
			Follow flag = followRepository.findByFromUserIdAndToUserId(from, to);
			if (flag == null) { follow = 2; }
			else if (flag.isFlag() == false) { follow = 3; }
			else if (flag.isFlag() == true) { follow = 4;}
		}

        User user = userRepository.findById(id).orElseThrow(()
				-> new IllegalArgumentException("해당 유저는 존재하지 않습니다."));
		List<UserCategory> userCategories = userCategoryRepository.findAllByUser(user);
		List<UserCategoryDto> userCategoryDtos = new ArrayList<>();
		for (UserCategory userCategory : userCategories) {
			userCategoryDtos.add(new UserCategoryDto(userCategory));
		}

		List<UserBoardDto> userBoardDtos = new ArrayList<>();
		List<Board> boards = boardRepository.findAllByUserId(id);
		HashMap<LocalDate, Long> dates = new HashMap<LocalDate, Long>();
		HashMap<Long, Long> categories = new HashMap<Long, Long>();

		for (Board board : boards) {
			userBoardDtos.add(new UserBoardDto(board));
			LocalDate date = board.getRegDttm().toLocalDate();
			Long categoryId = board.getCategory().getId();

			if (dates.containsKey(date)){
				dates.put(date, dates.get(date) + new Long(1));
			}
			else {
				dates.put(date, new Long(1));
			}

			if (categories.containsKey(categoryId)) {
				categories.put(categoryId, categories.get(categoryId) + new Long(1));
			}
			else {
				categories.put(categoryId, new Long(1));
			}
		}

		List<GroundBoardDto> groundDates = new ArrayList<>();
		Iterator<LocalDate> Dkeys = dates.keySet().iterator();
		while (Dkeys.hasNext()) {
			LocalDate Dkey = Dkeys.next();
			groundDates.add(new GroundBoardDto(Dkey, dates.get(Dkey)));
		}

		List<GroundCategoryDto> groundCategories = new ArrayList<>();
		Iterator<Long> Ckeys = categories.keySet().iterator();
		while (Ckeys.hasNext()) {
			Long Ckey = Ckeys.next();
			groundCategories.add(new GroundCategoryDto(Ckey, categories.get(Ckey)));
		}

		userProfileDto.setUser(user);
		userProfileDto.setFollow(follow);
		userProfileDto.setUserFollowerCount(followRepository.findFollowerCountById(id));
		userProfileDto.setUserFollowingCount(followRepository.findFollowingCountById(id));
		userProfileDto.setUserCategories(userCategoryDtos);
		userProfileDto.setUserBoardDtos(userBoardDtos);
		userProfileDto.setGroundDates(groundDates);
		userProfileDto.setGroundCategory(groundCategories);

        return userProfileDto;
    }

	// 프로필 업데이트 페이지
    @Transactional
    public UserUpdateDto getModifyUser(User loginUser) {
		UserUpdateDto userUpdateDto = new UserUpdateDto();

		userUpdateDto.setNickname(loginUser.getNickname());
		userUpdateDto.setPrivateYN(loginUser.isPrivateYN());
		userUpdateDto.setAge(loginUser.getAge());
		userUpdateDto.setGender(loginUser.getGender());
		userUpdateDto.setIntroduce(loginUser.getIntroduce());
		userUpdateDto.setUserImage(loginUser.getUserImage());

		return userUpdateDto;
    }

	// 프로필 업데이트
	@Transactional
    public void profileUpdate(User loginUser, UserUpdateDto entity) {

		loginUser.profileUpdate(entity, LocalDateTime.now());

    }

	// 관심종목 설정
	@Transactional
	public void setUserCategory(User loginUser, List<Long> userCategories) {

		userCategoryRepository.deleteAllByUser(loginUser);
		for (Long userCategoryId : userCategories) {
			Category category = categoryRepository.findById(userCategoryId).get();
			userCategoryRepository.save(new UserCategory(loginUser, category));
		}
	}


	// 회원 상세정보 추가
	@Transactional
	public void firstLogin(User loginUser, UserFirstLoginDto entity) {

		List<Long> userCategories = entity.getUserCategories();

		for (Long userCategoryId : userCategories) {
			Category category = categoryRepository.findById(userCategoryId).get();
			userCategoryRepository.save(new UserCategory(loginUser, category));
		}

		loginUser.firstLogin(entity);
	}

}
