package com.example.mef.demo.service;


import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServices  {

    @Autowired
    private UserRepository userRepository;


    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(username);
        if (user.isEmpty())
            throw new UsernameNotFoundException("User not found");
        return new org.springframework.security.core.userdetails.User(user.get().getFullName(), user.get().getPassword(), new ArrayList<>());

    }



    //Get All Users
    public List<User> getAllUser()
    {
        return this.userRepository.findAll();
    }

    //Get Single User
    public User getUser(int id)
    {
        Optional<User> optional = this.userRepository.findById(id);
        return optional.orElse(null);
    }

    //Get Single User By Email
    public Optional<User> getUserByEmail(String email)
    {
        Optional<User> user =	this.userRepository.findByEmail(email);
        return user;
    }

    //Update
    public void updateUser(User user, int id)
    {
//        user.setId(id);
        this.userRepository.save(user);
    }

    //delete single User
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