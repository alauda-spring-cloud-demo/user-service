package demo.ms.user.controller;

import demo.ms.common.entity.Authority;
import demo.ms.common.entity.User;
import demo.ms.common.entity.UserProjectRef;
import demo.ms.user.repository.AuthorityRepository;
import demo.ms.user.repository.UserProjectRefRepository;
import demo.ms.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserProjectRefRepository userProjectRefRepository;

    @Autowired
    AuthorityRepository authorityRepository;

    @PostMapping("/registry")
    public User create(@RequestBody User userInfo) throws Exception {

        User user = new User();
        user.setUsername(userInfo.getUsername());
        Example<User> userExample = Example.of(user);

        if(userRepository.findAll(userExample).size()>0){
            throw new Exception("用户名已经存在!");
        }
        user.setDisplayName(userInfo.getDisplayName());
        user.setMail(userInfo.getMail());
        user.setPassword(new BCryptPasswordEncoder().encode(userInfo.getPassword()));
        userRepository.save(user);

        Authority authority = new Authority();
        authority.setUsername(userInfo.getUsername());
        authority.setAuthority("ROLE_USER");

        authorityRepository.save(authority);
        user.setPassword(userInfo.getPassword());

        return user;
    }

    @GetMapping("/users/{id}")
    public User getUserInfo(@PathVariable String id){
        return userRepository.findOne(Long.valueOf(id));
    }

    @GetMapping("/users")
    public List<User> listUsersByProjectId(String projectId){
        if(org.apache.commons.lang.StringUtils.isNotEmpty(projectId)){
            return userRepository.getUsersByProjectId(Long.parseLong(projectId));
        }

        return userRepository.findAll();
    }

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

    @PostMapping("/user_project_ref")
    public UserProjectRef createUserProjectRef(@RequestBody UserProjectRef userProjectRef) throws Exception {

        if(userProjectRefRepository.findAll(Example.of(userProjectRef)).size()>0){
            throw new Exception("该用户已经在项目中");
        }

        return userProjectRefRepository.save(userProjectRef);
    }

    @DeleteMapping("/user_project_ref")
    public void deleteUserProjectRef(String projectId,String userId){
        UserProjectRef userProjectRef = new UserProjectRef();
        userProjectRef.setProjectId(Long.parseLong(projectId));
        userProjectRef.setUserId(Long.parseLong(userId));

        UserProjectRef res = userProjectRefRepository.findOne(Example.of(userProjectRef));

        if(res!=null){
            userProjectRefRepository.delete(res);
        }
    }

    @PutMapping("/users")
    public void update(@RequestBody User user){

        Example<User> userExample = Example.of(user);

        if(userRepository.findOne(userExample)!=null){
            userRepository.save(user);
        }
    }
}
