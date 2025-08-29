package com.hgl.example.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName: User
 * @Package: com.hgl.example.common.model
 * @Description:
 * @Author HGL
 * @Create: 2025/8/29 14:42
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
}
