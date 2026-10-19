package com.jetwork.bin;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude() //각 항목중 null 일경우 json 리턴 할때 안보냄
public class ResJson {
	private String name;
	private int age;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
}