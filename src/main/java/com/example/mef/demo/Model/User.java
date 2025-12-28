package com.example.mef.demo.Model;

import com.example.mef.demo.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;


@Setter
@Getter
@Entity
@Table(name="User")
@NoArgsConstructor
public class User implements UserDetails {
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }

    // Getters and setters
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NotEmpty
    @Column(name="password", nullable = false, length = 250)
    @Size(min=3,max=250 , message = "Password must be 3 - 15 characters long")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotEmpty
    @Column(name="username", nullable = false, length = 100)
    @Size(min=4,message = "Username must be minimum of 4 characters")
    private String username;

    @Email(message = "Email address is not valid")
    @Column(name="email", nullable = false,unique = true,
            length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    private Role role;

    // Constructor with parameters
    public User(String username,String password, String email) {
        this.username = username;
        this.email = email;
        this.password=password;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        System.out.println("eeeeeeeeeeeeeeeeeeee");
        if (this.role == Role.ADMIN) {
            return List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("STANDARD"));
        }
        return List.of(new SimpleGrantedAuthority("STANDARD"));



    }
   /* @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns=@JoinColumn(name="user",referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name="role", referencedColumnName = "id")
    )
    private Set<Role> roles= new HashSet<>();

    // methods from UserDetails as required by Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = this.roles.stream().map((role) -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
        return authorities;
    }*/

}