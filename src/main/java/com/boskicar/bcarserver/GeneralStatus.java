package com.boskicar.bcarserver;

import java.time.LocalDateTime;

public class GeneralStatus
{	
	public static enum OrderType 
	{
		STOP,FORDWARD,BACKWARD,LEFT,RIGHT
	}
	
	public static enum Status 
	{
		ON,OFF,BLINK
	}
	
	private int lastLRDesiredAngle;
	
	private int lastFBOrderSpeed;
	
	private LocalDateTime lastFBOrderDate = LocalDateTime.now().minusYears(1);
	
	private OrderType lastFBOrderType;
	
	private LocalDateTime lastLROrderDate = LocalDateTime.now().minusYears(1);
	
	private OrderType lastLROrderType;
	
	private Status engineControl;

	private Status steeringWheelControl;
	
	private Status lights;
	
	private Status mobileControl;
	
	private Status fans;
	
	public Status getEngineControl() {
		return engineControl;
	}

	public void setEngineControl(Status engineControl) {
		this.engineControl = engineControl;
	}

	public Status getSteeringWheelControl() {
		return steeringWheelControl;
	}

	public void setSteeringWheelControl(Status steeringWheelControl) {
		this.steeringWheelControl = steeringWheelControl;
	}

	public Status getMobileControl() {
		return mobileControl;
	}

	public void setMobileControl(Status mobileControl) {
		this.mobileControl = mobileControl;
	}
	
	public Status getFans() {
		return fans;
	}

	public void setFans(Status fans) {
		this.fans = fans;
	}

	public Status getLights() {
		return lights;
	}

	public void setLights(Status lights) {
		this.lights = lights;
	}

	public LocalDateTime getLastFBOrderDate() {
		return lastFBOrderDate;
	}

	public void setLastFBOrderDate(LocalDateTime lastFBOrderDate) {
		this.lastFBOrderDate = lastFBOrderDate;
	}

	public OrderType getLastFBOrderType() {
		return lastFBOrderType;
	}

	public void setLastFBOrderType(OrderType lastFBOrderType) {
		this.lastFBOrderType = lastFBOrderType;
	}

	public LocalDateTime getLastLROrderDate() {
		return lastLROrderDate;
	}

	public void setLastLROrderDate(LocalDateTime lastLROrderDate) {
		this.lastLROrderDate = lastLROrderDate;
	}

	public OrderType getLastLROrderType() {
		return lastLROrderType;
	}

	public void setLastLROrderType(OrderType lastLROrderType) {
		this.lastLROrderType = lastLROrderType;
	}

	
	public int getLastFBOrderSpeed() {
		return lastFBOrderSpeed;
	}

	public void setLastFBOrderSpeed(int lastFBOrderSpeed) {
		this.lastFBOrderSpeed = lastFBOrderSpeed;
	}
	
	public int getLastLRDesiredAngle() {
		return lastLRDesiredAngle;
	}

	public void setLastLRDesiredAngle(int lastLRDesiredAngle) {
		this.lastLRDesiredAngle = lastLRDesiredAngle;
	}

	@Override
	public String toString() {
		return "GeneralStatus [lastLRDesiredAngle=" + lastLRDesiredAngle + ", lastFBOrderSpeed=" + lastFBOrderSpeed
				+ ", lastFBOrderDate=" + lastFBOrderDate + ", lastFBOrderType=" + lastFBOrderType + ", lastLROrderDate="
				+ lastLROrderDate + ", lastLROrderType=" + lastLROrderType + ", mobileControl=" + mobileControl
				+ ", engineControl=" + engineControl + ", steeringWheelControl=" + steeringWheelControl + ", lights="
				+ lights + ", fans=" + fans + "]";
	}
}