package com.example.mef.demo.Service;


import com.example.mef.demo.Repository.UserRepository;
import com.example.mef.demo.response.CustomResponse;
import org.springframework.beans.factory.annotation.Autowired;


import com.example.mef.demo.Model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServices  implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;


    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null)
            throw new UsernameNotFoundException("User not found");
        return  new org.springframework.security.core.userdetails.User(user.getUsername(), (String) user.getPassword(), user.getAuthorities());

    }



    //Get All Users
    public List<User> getAllUser()
    {
        List<User> users = (List<User>) this.getAllUser();
        return users;
    }

    //Get Single User
    public User getUser(int id)
    {
        Optional<com.example.mef.demo.Model.User> optional = this.userRepository.findById(id);
        com.example.mef.demo.Model.User user = optional.get();
        return user;
    }

    //Get Single User By Email
    public User getUserByEmail(String email)
    {
        User user=	this.userRepository.findByUsername(email);
        return user;
    }

    //Update
    public void updateUser(User user,int id)
    {
        user.setId(id);
        this.userRepository.save(user);
    }

    //delete single User
   // @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(int id)
    {

        this.userRepository.deleteById(id);

    }

    //Add User
    public void addUser(User user)
    {
        this.userRepository.save(user);
    }


    public boolean validateLoginCredentials(String email,String password)
    {
        List<User> users = (List<User>) this.userRepository.findAll();
        for(User u:users)
        {
            if(u!=null && u.getPassword().equals(password) && u.getEmail().equals(email))
            {
                return true;
            }
        }
        return false;
    }

}