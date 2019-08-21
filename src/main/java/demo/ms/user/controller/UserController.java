package demo.ms.user.controller;

import com.google.common.collect.Sets;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import demo.ms.common.entity.*;
import demo.ms.common.stream.LoggerEventSink;
import demo.ms.common.vo.JwtUserInfo;
import demo.ms.user.repository.AuthorityRepository;
import demo.ms.user.repository.ProjectRepository;
import demo.ms.user.repository.UserProjectRefRepository;
import demo.ms.user.repository.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

@EnableBinding(LoggerEventSink.class)
@RestController
public class UserController {

    @Autowired
    LoggerEventSink loggerEventSink;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthorityRepository authorityRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserProjectRefRepository userProjectRefRepository;

    @HystrixCommand(commandKey = "CreateUser")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/users")
    public User create(@RequestBody User userInfo) throws Exception {
        User user = new User();
        user.setUsername(userInfo.getUsername());
        Example<User> userExample = Example.of(user);

        if(userRepository.findAll(userExample).size()>0){
            throw new Exception("用户名已经存在!");
        }
        userInfo.setPassword(new BCryptPasswordEncoder().encode(userInfo.getPassword()));
        userInfo.setAccountNonExpired(true);
        userInfo.setAccountNonLocked(true);
        userInfo.setCredentialsNonExpired(true);
        userInfo.setEnabled(true);

        userRepository.save(userInfo);
        user.setPassword(null);

        return user;
    }

    @HystrixCommand(commandKey = "UserRegistry")
    @PostMapping("/registry")
    public User registry(@RequestBody User userInfo) throws Exception {

        User user = new User();
        user.setUsername(userInfo.getUsername());
        Example<User> userExample = Example.of(user);

        if(userRepository.findAll(userExample).size()>0){
            throw new Exception("用户名已经存在!");
        }
        userInfo.setPassword(new BCryptPasswordEncoder().encode(userInfo.getPassword()));
        user.setAuthorities(Sets.newHashSet(new Authority(null,
                userInfo.getUsername(),userInfo)));
        userInfo.setAccountNonExpired(true);
        userInfo.setAccountNonLocked(true);
        userInfo.setCredentialsNonExpired(true);
        userInfo.setEnabled(true);
        userRepository.save(userInfo);
        userInfo.setPassword(null);

        return userInfo;
    }

//    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_PM','ROLE_PMO')")
    @HystrixCommand(commandKey = "ListUser")
    @GetMapping("/users/list")
    public Page<User> listAllUsers(Integer page,Integer size){
        page = page == null ? 0:page;
        size = size == null ? 10:size;
        Page<User> userPage = userRepository.findAll(new PageRequest(page,size ,Sort.Direction.DESC,"id"));
        return userPage;
    }

    @HystrixCommand(commandKey = "GetUserInfo")
    @GetMapping("/users/{id}")
    public User getUserInfo(@PathVariable String id){
        return userRepository.findOne(Long.valueOf(id));
    }

    @HystrixCommand(commandKey = "ListUsersByProject")
    @GetMapping("/users")
    public List<User> listUsersByProjectId(String projectId){
        if(org.apache.commons.lang.StringUtils.isNotEmpty(projectId)){
            return userRepository.getUsersByProjectId(Long.parseLong(projectId));
        }

        return userRepository.findAll();
    }

    @HystrixCommand(commandKey = "ListProjectsByUser")
    @GetMapping("/user_project_ref")
    public Long[] listProjectIdsByUserId(String userId){
        UserProjectRef userProjectRef = new UserProjectRef();
        userProjectRef.setUserId(Long.parseLong(userId));
        Example<UserProjectRef> userProjectRefExample = Example.of(userProjectRef);

        List<UserProjectRef> userProjectRefList = userProjectRefRepository.findAll(userProjectRefExample).stream()
                .collect(Collectors.toList());

        Long[] ids = userProjectRefList.stream().map(o->o.getProjectId()).toArray(Long[]::new);

        return ids;
    }

    @HystrixCommand(commandKey = "ListPM")
    @GetMapping("/users/pm")
    public List<User> getPMList(){
        return userRepository.getUsersByRole("ROLE_PM");
    }

    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN','ROLE_PM')")
    @PostMapping("/user_project_ref")
    public UserProjectRef createUserProjectRef(@RequestBody UserProjectRef userProjectRef) throws Exception {
        JwtUserInfo jwtUserInfo =  (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Project project = projectRepository.findOne(userProjectRef.getProjectId());

        User user = userRepository.getOne(userProjectRef.getUserId());

        if(user==null){
            throw new Exception("用户不存在");
        }

        if(project==null){
            throw new Exception("项目不存在或已经删除");
        }

        if(userProjectRefRepository.findAll(Example.of(userProjectRef)).size()>0){
            throw new Exception("该用户已经在项目中");
        }

        Message msg1 = new Message(
                null,
                user.getId(),
                Long.valueOf(userProjectRef.getProjectId()),
                "PROJECT",
                String.format("[%s]将你添加到[%s]项目中",jwtUserInfo.getLoginName(),project.getName()),
                new Date
                        (System
                                .currentTimeMillis()));

        Message msg2 = new Message(
                null,
                null,
                Long.valueOf(userProjectRef.getProjectId()),
                "PROJECT",
                String.format("[%s]将[%s]添加到[%s]项目中",jwtUserInfo.getLoginName(),user.getUsername(),project.getName()),
                new Date
                (System
                .currentTimeMillis()));

        loggerEventSink.output().send(MessageBuilder.withPayload(msg1).build());
        loggerEventSink.output().send(MessageBuilder.withPayload(msg2).build());

        return userProjectRefRepository.save(userProjectRef);
    }

    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN','ROLE_PM')")
    @DeleteMapping("/user_project_ref")
    public void deleteUserProjectRef(String projectId,String userId){
        JwtUserInfo jwtUserInfo =  (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserProjectRef userProjectRef = new UserProjectRef();
        userProjectRef.setProjectId(Long.parseLong(projectId));
        userProjectRef.setUserId(Long.parseLong(userId));

        Project project = projectRepository.findOne(userProjectRef.getProjectId());

        User user = userRepository.getOne(userProjectRef.getUserId());

        UserProjectRef res = userProjectRefRepository.findOne(Example.of(userProjectRef));

        if(res!=null){
            userProjectRefRepository.delete(res);
        }

        if(project!=null && user!=null){
            Message msg1 = new Message(
                    null,
                    user.getId(),
                    Long.valueOf(userProjectRef.getProjectId()),
                    "PROJECT",
                    String.format("[%s]将你从[%s]项目移除",jwtUserInfo.getLoginName(),project.getName()),
                    new Date
                            (System
                                    .currentTimeMillis()));

            Message msg2 = new Message(
                    null,
                    null,
                    Long.valueOf(userProjectRef.getProjectId()),
                    "PROJECT",
                    String.format("[%s]将[%s]从[%s]项目中移除",jwtUserInfo.getLoginName(),user.getUsername(),project.getName
                            ()),
                    new Date
                            (System
                                    .currentTimeMillis()));

            loggerEventSink.output().send(MessageBuilder.withPayload(msg1).build());
            loggerEventSink.output().send(MessageBuilder.withPayload(msg2).build());
        }
    }

    @Transactional
    @PutMapping("/users")
    public ResponseEntity update(@RequestBody User user) throws Exception {
        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(StringUtils.equals(jwtUserInfo.getUserId(),String.valueOf(user.getId()))){
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        if(user.getAuthorities() == null || user.getAuthorities().size() == 0){
            throw new Exception("用户至少要有一个角色");
        }

        User oldUser = userRepository.findOne(user.getId());

        if(oldUser!=null){

            oldUser.getAuthorities().clear();
            userRepository.saveAndFlush(oldUser);

            if(user.getDisplayName()!=null){
                oldUser.setDisplayName(user.getDisplayName());
            }

            if(user.getMail()!=null){
                oldUser.setMail(user.getMail());
            }

            if(user.getPassword()!=null){
                oldUser.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            }

            if(user.getAuthorities()!=null && user.getAuthorities().size()>0){
                user.getAuthorities().stream().forEach(o->{
                    Authority authority = new Authority();
                    authority.setAuthority(o.getAuthority());
                    authority.setUser(oldUser);
                    oldUser.getAuthorities().add(authority);
                });
            }

            userRepository.save(oldUser);

            return new ResponseEntity(oldUser,HttpStatus.OK);
        }else{
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }
}
