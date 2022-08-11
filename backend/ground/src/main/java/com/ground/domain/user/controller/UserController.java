package com.ground.domain.user.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ground.domain.jwt.JwtTokenProvider;
import com.ground.domain.user.dto.*;
import com.ground.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.ground.domain.jwt.TokenResponse;
import com.ground.domain.user.entity.User;
import com.ground.domain.user.service.KakaoService;
import com.ground.domain.user.service.MailSendService;
import com.ground.domain.user.service.UserService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;


@Log4j2
@ApiResponses(value = { 
		@ApiResponse(code = 200, message = "Success"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error")
})
@RestController
@RequestMapping("/rest/user")
//@CrossOrigin(allowCredentials = "*", originPatterns = { "*" })
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    @Autowired
    JwtTokenProvider jwtTokenProvider;
    @Autowired
    UserRepository userRepository;
	@Autowired
	UserService userService;
	@Autowired
	MailSendService mailService;
	@Autowired
	KakaoService kakaoService;

	
	@PersistenceContext
	EntityManager em;
	
	@PostMapping("/signUp")
    @ApiOperation(value = "회원가입", response = boolean.class)
    public boolean signUp(@RequestBody UserRegisterDto params){
    	return userService.saveUser(params);
    }
    
    @GetMapping("/isUsedUsername")
    @ApiOperation(value = "아이디 중복 체크", response = boolean.class)
    public boolean isUsedUsername(String username){
        return userService.checkUsername(username);
    }
    
    @GetMapping("/isUsedNickname")
    @ApiOperation(value = "닉네임 중복 체크", response = boolean.class)
    public boolean isUsedNickname(String nickname){
        return userService.checkNickname(nickname);
    }
    
    @GetMapping("/isUsedEmail")
    @ApiOperation(value = "이메일 중복 체크", response = boolean.class)
    public boolean isUsedEmail(String email){
        return userService.checkEmail(email);
    }
    
    @GetMapping("/emailAuth")
    @ApiOperation(value = "이메일 인증", response = String.class)
    public String emailAuth(String email) throws UnsupportedEncodingException{
        return mailService.joinEmail(email);
    }
    
    @PutMapping("/deleteUser")
    @ApiOperation(value = "회원 탈퇴", response = String.class)
    public boolean deleteUser(Long id){
        return userService.deleteUser(id);
    }
    
    @GetMapping("/findId/{email}")
    @ApiOperation(value = "아이디 찾기", response = String.class)
    public String findId(@PathVariable String email) {
    	return userService.findId(email);
    }
    
    @PostMapping("/modifyPass")
    @ApiOperation(value = "비밀번호 변경을 위한 아이디, 이메일 확인", response = boolean.class)
    public boolean modifyPassCheck(@RequestBody UserFindPassDto params) {
    	return userService.modifyPassCheck(params);
    }
    
    @PutMapping("/modifyPass")
    @ApiOperation(value = "비밀번호 변경", response = boolean.class)
    //@RequestHeader String header, 
    public boolean modifyPass(@RequestBody UserModifyPassDto params) {
    	return userService.modifyPass(params);
    }
    
    
    @PostMapping("/login")
    @ApiOperation(value = "로그인", response = UserStateDto.class)
    public UserLoginResponseDto login(@RequestBody UserLoginDto params){
    	return userService.login(params);
    }

    @GetMapping("/state")
    @ApiOperation(value = "유저상태정보 전송", response = UserStateDto.class)
    public UserStateDto userState(@RequestHeader String Authorization) {
    	String ftoken = Authorization.substring(7);
    	return userService.userState(ftoken);
    }
    
    @RequestMapping("/oauth/kakao")
    @ApiOperation(value = "카카오 로그인", response = String.class)
    public UserLoginResponseDto kakaoLogin(@RequestParam("code") String code, HttpSession session) throws IOException {
    	log.info(code);
    	String access_Token = KakaoService.getAccessToken(code);
    	
    	// 카카오 user 정보 받아오기
    	// 객체로 받기
    	UserKakaoLoginDto ukld = new UserKakaoLoginDto();
    	ukld = kakaoService.getUserInfo(access_Token);
    	System.out.println("uc ukld: " + ukld);
    	return kakaoService.kakaoLogin(ukld);
    	
    	// 아래 로직 수행
    	// 현재 DB에 해당 이메일이 없으면 회원가입을 시키고, 아니면 로그인시킨다.
    	// username(id) -> id(long), email -> 이메일
    	
    }
    
    @DeleteMapping("/logout")
    @ApiOperation(value = "로그아웃", response = boolean.class)
    public boolean logoutUser(@RequestHeader String Authorization) {
    	String ftoken = Authorization.substring(7);
    	return userService.logoutUser(ftoken);
    }

    

    // -----------------BSH-----------------
    // 프로필 조회 이동
    @GetMapping("/profile/{userId}")
    @ApiOperation(value = "프로필 조회", response = String.class)
    public UserProfileDto userProfile(@PathVariable Long userId, @RequestHeader String Authorization) {
    	String ftoken = Authorization.substring(7);
        User user = userRepository.findByUsername(jwtTokenProvider.getSubject(ftoken)).get();
        Long loginUserId = user.getId();

        return userService.getUserProfile(userId, loginUserId);
    }

    // 회원 정보 수정 페이지로 이동
    @GetMapping("/modifyUser")
    @ApiOperation(value = "회원정보 수정 페이지로 이동", response = String.class)
    public UserUpdateDto getModifyUser(@RequestHeader String Authorization){
    	String ftoken = Authorization.substring(7);
        User user = userRepository.findByUsername(jwtTokenProvider.getSubject(ftoken)).get();

        return userService.getModifyUser(user);
    }

    // 회원 정보 수정
    @PutMapping("/modifyUser")
    @ApiOperation(value = "회원정보 수정", response = String.class)
    public void modifyUser(@RequestHeader String Authorization, @RequestBody UserUpdateDto userUpdateDto) {
    	String ftoken = Authorization.substring(7);
        User user = userRepository.findByUsername(jwtTokenProvider.getSubject(ftoken)).get();

        userService.profileUpdate(user, userUpdateDto);
    }

    // 회원 상세정보 추가
    @PostMapping("/userDetail")
    @ApiOperation(value = "회원 상세정보 추가")
    public void firstLogin(@RequestHeader String Authorization, @RequestBody UserFirstLoginDto userFirstLoginDto) {
    	String ftoken = Authorization.substring(7);
        User loginUser = userRepository.findByUsername(jwtTokenProvider.getSubject(ftoken)).get();

        userService.firstLogin(loginUser, userFirstLoginDto);
    }

}