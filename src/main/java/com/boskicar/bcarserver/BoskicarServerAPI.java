package com.boskicar.bcarserver;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.io.File;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boskicar.bcarserver.GeneralStatus.OrderType;
import com.boskicar.bcarserver.GeneralStatus.Status;
import com.pi4j.gpio.extension.ads.ADS1015GpioProvider;
import com.pi4j.gpio.extension.ads.ADS1015Pin;
import com.pi4j.gpio.extension.ads.ADS1x15GpioProvider.ProgrammableGainAmplifierValue;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.i2c.I2CBus;

@RestController
public class BoskicarServerAPI implements InitializingBean 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(BoskicarServerAPI.class);
	
	private static final int MIN_FB_STRENGTH = 25;
	
	private static final int NEAR_MAX_FB_STRENGTH = 95;
	
	private static final int MAX_FB_STRENGTH = 100;
	
	private static final int MIN_LR_STRENGTH = 25;
	
	private static final int MAX_LR_STRENGTH = 100;
	
	private static final int STEP_LR_STRENGTH = 1;
	
	private static final double DG000 = 0.0d;
	
	private static final double DG180 = 180.0d;
	
	private static final double ERROR_ANGLE = 10.0d;
	
	private DecimalFormat pwmValueFormat = new DecimalFormat("#.##");  

//fb vars
	
	private boolean engineControl = true;
	
	private Object lastFBOrderLock = new Object();
	
	private int lastFBOrderSpeed = 0;
	
	private LocalDateTime lastFBOrderDate = LocalDateTime.now().minusYears(1);
	
	private OrderType lastFBOrderType = OrderType.STOP;
	
	private OrderType previousFBProcessedOrderType = null;
	
//lr vars
	
	private boolean steeringWheelControl = true;
	
	private Object lastLROrderLock = new Object();
		
	private Status throttleControl = Status.ON;

	private int lastLRDesiredAngle = 0;
	
	private LocalDateTime lastLROrderDate = LocalDateTime.now().minusYears(1);
	
	private OrderType lastLROrderType = OrderType.STOP;
	
	private int lrStrength = 0;

	private String jarPath = "/usr/local/bin/bcarserver.jar";

	private OrderType previousLRProcessedOrderType = null;
	
//pi4j
	
	private GpioController gpio;
	
    private GpioPinDigitalOutput relayMobileCrlMotor1a;
	
	private GpioPinDigitalOutput relayMobileCrlMotor1b;
	
	private GpioPinDigitalOutput relayMobileCrlMotor2a;
		
	private GpioPinDigitalOutput relayMobileCrlMotor2b;
	
	private GpioPinDigitalOutput relayMobileCrlMainPower;
	
	private GpioPinDigitalOutput relayMobileCrlMainGround;
	
	private GpioPinDigitalOutput relayLights;
	
	private GpioPinDigitalOutput relayFans;
	
	private ADS1015GpioProvider ads1015GpioProvider;
	
	private double steeringWheelSensorCurrentValue;
	
	private double steeringWheelSensorMaxLeftValue;
	
	private double steeringWheelSensorMaxRightValue;
	
	private double steeringWheelSensorCenterValue;

//pi-blaster	
	
	private Object piBlasterWriteLock = new Object();

	private PrintWriter piBlasterPrintWriter = null; 
	
	private final String PI_BLASTER_FILEPATH = "/dev/pi-blaster";

	private static final String PI_BLASTER_FB_CMD = "12=1 26=";
	
	private static final String PI_BLASTER_STOP_FB_CMD = "12=0 26=0";

	private static final String PI_BLASTER_LR_CMD = "13=1 24=";

	private static final String PI_BLASTER_STOP_LR_CMD = "13=0 24=0";
	
	@PreDestroy
    public void tearDown() throws Exception 
	{
		if(LOGGER.isDebugEnabled()) LOGGER.debug("shutting down GPIOs...");
		
		try 
		{
			gpio.shutdown();
		} 
		catch (Exception e) 
		{
			//NOP
		}
		
		try 
		{
			piBlasterPrintWriter.close();
		} 
		catch (Exception e) 
		{	
			//NOP
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception 
	{
		try 
		{
			piBlasterPrintWriter = new PrintWriter(new FileOutputStream(PI_BLASTER_FILEPATH), true);
			
			gpio = GpioFactory.getInstance();
			gpio.setShutdownOptions(true, PinState.LOW);
			
			//PI-BLASTER USES: RaspiPin.GPIO_26,RaspiPin.GPIO_23,RaspiPin.GPIO_05,RaspiPin.GPIO_25

			relayMobileCrlMotor1a = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_28, PinState.LOW);//blanco1
			relayMobileCrlMotor1b = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, PinState.LOW);//gris2
			
			relayMobileCrlMotor2a = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_15, PinState.LOW);//violeta3
			relayMobileCrlMotor2b = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_16, PinState.LOW);//azul4

			relayMobileCrlMainPower = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);//verde5
			relayMobileCrlMainGround = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, PinState.LOW);//amarillo6
			
			relayLights = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13, PinState.LOW);//marron8
			relayFans = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12, PinState.LOW);//naranja7
			
			ads1015GpioProvider  = new ADS1015GpioProvider(I2CBus.BUS_1, ADS1015GpioProvider.ADS1015_ADDRESS_0x48);
			
			// provision gpio analog input pins from ADS1015
	        GpioPinAnalogInput analogInput [] = {
	                gpio.provisionAnalogInputPin(ads1015GpioProvider, ADS1015Pin.INPUT_A0, "steeringWheelPositionSensor")
	            };

	        // ATTENTION !!
	        // It is important to set the PGA (Programmable Gain Amplifier) for all analog input pins.
	        // (You can optionally set each input to a different value)
	        // You measured input voltage should never exceed this value!
	        // PGA value PGA_4_096V is a 1:1 scaled input,
	        // so the output values are in direct proportion to the detected voltage on the input pins
	        ads1015GpioProvider.setProgrammableGainAmplifier(ProgrammableGainAmplifierValue.PGA_4_096V, ADS1015Pin.ALL);

	        // Define a threshold value for each pin for analog value change events to be raised.
	        // It is important to set this threshold high enough so that you don't overwhelm your program with change events for insignificant changes
	        ads1015GpioProvider.setEventThreshold(50, ADS1015Pin.ALL);

	        // Define the monitoring thread refresh interval (in milliseconds).
	        // This governs the rate at which the monitoring thread will read input values from the ADC chip
	        // (a value less than 50 ms is not permitted)
	        ads1015GpioProvider.setMonitorInterval(50);

	        // create analog pin value change listener
	        GpioPinListenerAnalog listener = new GpioPinListenerAnalog()
	        {
	            @Override
	            public void handleGpioPinAnalogValueChangeEvent(GpioPinAnalogValueChangeEvent event)
	            {
	            	steeringWheelSensorCurrentValue = event.getValue();
	            }
	        };

	        analogInput[0].addListener(listener);
	        
	        try
	        {
		        mobilecontrol(Status.OFF); 
				Thread.sleep(2000);
				calibrate();	
	        }
	        catch(Exception e)
	        {
	        	LOGGER.error("Error calibrating steering wheel", e);
	        }
	               
		} 
		catch (Exception e) 
		{
			LOGGER.error("Error init gpios", e);
		}
	}
			
	private void piBlasterWrite(String cmd)
	{
		synchronized (piBlasterWriteLock) 
		{
			if(piBlasterPrintWriter != null)
			{
				piBlasterPrintWriter.println(cmd);
			}
		}	
	}
	
	@Scheduled(fixedRate = 150)
	private void processFBOrders()
	{
		synchronized (lastFBOrderLock) 
		{
			try 
			{
				long millis = ChronoUnit.MILLIS.between(lastFBOrderDate, LocalDateTime.now());
				
				if(LOGGER.isDebugEnabled()) LOGGER.debug("processFBOrders: previousOrderType=[{}] lastOrderType=[{}] millis=[{}] lastOrderDate=[{}]", previousFBProcessedOrderType, lastFBOrderType, millis, lastFBOrderDate);

				if(millis > 300)
				{
					if(!OrderType.STOP.equals(previousFBProcessedOrderType))
					{	
						previousFBProcessedOrderType = internal_fb_stop();
						lastFBOrderType = OrderType.STOP;
					}
				}
				else
				{
					switch (lastFBOrderType) 
					{
						case FORDWARD: 
						{	
							previousFBProcessedOrderType = internal_forward(); 
							break;
						}
						case BACKWARD: 
						{	
							previousFBProcessedOrderType = internal_backward(); 
							break;
						}
						default:
						{
							previousFBProcessedOrderType = internal_fb_stop(); 
							break;
						}
					}
				}
			} 
			catch(Throwable e)
			{
				LOGGER.error("processFBOrders error", e);				
			}
		}
	}

	@Scheduled(fixedRate = 150)
	private void processLROrders()
	{
		synchronized (lastLROrderLock) 
		{
			try 
			{
				long millis = ChronoUnit.MILLIS.between(lastLROrderDate, LocalDateTime.now());
				
				if(LOGGER.isDebugEnabled()) LOGGER.debug("processLROrders: previousOrderType=[{}] lastOrderType=[{}] millis=[{}] lastOrderDate=[{}]", previousLRProcessedOrderType, lastLROrderType, millis, lastLROrderDate);

				if(millis > 300)
				{
					if(!OrderType.STOP.equals(previousLRProcessedOrderType))
					{	
						previousLRProcessedOrderType = internal_lr_stop();
						lastLROrderType = OrderType.STOP;
						lrStrength = MIN_LR_STRENGTH;
					}
				}
				else if(Status.ON.equals(throttleControl))
				{
					switch (lastLROrderType) 
					{
						case RIGHT: 
						{	
							previousLRProcessedOrderType = internal_right(lrStrength);
							break;
						}
						case LEFT: 
						{	
							previousLRProcessedOrderType = internal_left(lrStrength); 
							break;
						}
						default:
						{
							previousLRProcessedOrderType = internal_lr_stop();
							break;
						}
					}
				}
				else
				{					
					double equivalentSteeringWheelSensorValue = getEquivalentSteeringWheelSensorValue(lastLRDesiredAngle);
					double steeringWheelCurrentSensorAngle = getSteeringWheelCurrentSensorAngle();
					
					double minErrorSwCurrentSensorAngle = Math.max(steeringWheelCurrentSensorAngle-ERROR_ANGLE, DG000);
					double maxErrorSwCurrentSensorAngle = Math.min(steeringWheelCurrentSensorAngle+ERROR_ANGLE, DG180);
			
					boolean currentPositionFlag =  ((minErrorSwCurrentSensorAngle <= lastLRDesiredAngle) && (lastLRDesiredAngle <= maxErrorSwCurrentSensorAngle));
					
					if(currentPositionFlag)
					{
						LOGGER.info("processLROrders: lastLRDesiredAngle=[{}] minErrorSwCurrentSensorAngle=[{}] steeringWheelCurrentSensorAngle=[{}] maxErrorSwCurrentSensorAngle=[{}] currentPositionFlag=[{}] lrStrength=[{}] equivalentSteeringWheelSensorValue=[{}]", lastLRDesiredAngle, minErrorSwCurrentSensorAngle, steeringWheelCurrentSensorAngle, maxErrorSwCurrentSensorAngle,  currentPositionFlag, lrStrength, equivalentSteeringWheelSensorValue);
						previousLRProcessedOrderType = internal_lr_stop();
						lrStrength = MIN_LR_STRENGTH;
					}
					else
					{
						lrStrength += STEP_LR_STRENGTH;
						
						if(lrStrength > MAX_LR_STRENGTH)
						{
							lrStrength = MIN_LR_STRENGTH;
						}
										
						if(steeringWheelCurrentSensorAngle < lastLRDesiredAngle)
						{
							LOGGER.info("processLROrders: LEFT lastLRDesiredAngle=[{}] minErrorSwCurrentSensorAngle=[{}] steeringWheelCurrentSensorAngle=[{}] maxErrorSwCurrentSensorAngle=[{}] currentPositionFlag=[{}] lrStrength=[{}] equivalentSteeringWheelSensorValue=[{}]", lastLRDesiredAngle, minErrorSwCurrentSensorAngle, steeringWheelCurrentSensorAngle, maxErrorSwCurrentSensorAngle, currentPositionFlag, lrStrength, equivalentSteeringWheelSensorValue);
							previousLRProcessedOrderType = internal_left(lrStrength); 
						}
						else if (steeringWheelCurrentSensorAngle > lastLRDesiredAngle)
						{
							LOGGER.info("processLROrders: RIGHT lastLRDesiredAngle=[{}] minErrorSwCurrentSensorAngle=[{}] steeringWheelCurrentSensorAngle=[{}] maxErrorSwCurrentSensorAngle=[{}] currentPositionFlag=[{}] lrStrength=[{}] equivalentSteeringWheelSensorValue=[{}]", lastLRDesiredAngle, minErrorSwCurrentSensorAngle, steeringWheelCurrentSensorAngle, maxErrorSwCurrentSensorAngle, currentPositionFlag, lrStrength, equivalentSteeringWheelSensorValue);
							previousLRProcessedOrderType = internal_right(lrStrength); 
						}
					}
				}
			} 
			catch(Throwable e)
			{
				LOGGER.error("processLROrders error", e);				
			}
		}
	}
	
	private OrderType internal_forward()
	{	
		String pwmValue = this.lastFBOrderSpeed == 0 ? "0" : pwmValueFormat.format(((((double)this.lastFBOrderSpeed)/100d)/2d)+0.5d);
		if(LOGGER.isDebugEnabled()) LOGGER.debug("internal_forward lastFBOrderSpeed=[{}] pwmValue=[{}]", lastFBOrderSpeed, pwmValue);
		piBlasterWrite(PI_BLASTER_FB_CMD+pwmValue);
		return OrderType.FORDWARD;
	}
		
	private OrderType  internal_backward()
	{
		String pwmValue = this.lastFBOrderSpeed == 0 ? "0" : pwmValueFormat.format(0.5d-((((double)this.lastFBOrderSpeed)/100d)/2d));
		if(LOGGER.isDebugEnabled()) LOGGER.debug("internal_backward lastFBOrderSpeed=[{}] pwmValue=[{}]", lastFBOrderSpeed, pwmValue);
		piBlasterWrite(PI_BLASTER_FB_CMD+pwmValue);
		return OrderType.BACKWARD;
	}
		
	private OrderType  internal_fb_stop()
	{
		piBlasterWrite(PI_BLASTER_STOP_FB_CMD);	
		return OrderType.STOP;
	}
	
	@RequestMapping(value = "/throttle/{fbOrderSpeed}/{lrStrength}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void throttle(@PathVariable("fbOrderSpeed") int fbOrderSpeed, @PathVariable("lrStrength") int lrStrength)
	{
		this.throttleControl = Status.ON;

		this.lastLROrderType = (lrStrength == 0 ? OrderType.STOP : (lrStrength > 0 ? OrderType.RIGHT  : OrderType.LEFT));
		this.lrStrength = Math.abs(lrStrength);
		lastLROrderDate = LocalDateTime.now();

		this.lastFBOrderType = (fbOrderSpeed == 0 ? OrderType.STOP : (fbOrderSpeed > 0 ? OrderType.FORDWARD  : OrderType.BACKWARD));
		this.lastFBOrderSpeed = Math.abs(fbOrderSpeed);
		lastFBOrderDate  = LocalDateTime.now();
	}

	private OrderType internal_left(int strength)
	{	
		String pwmValue = strength == 0 ? "0" : pwmValueFormat.format(0.5d-((((double)strength)/100d)/2d));
		if(LOGGER.isDebugEnabled()) LOGGER.debug("internal_left strength=[{}] pwmValue=[{}]", strength, pwmValue);
		piBlasterWrite(PI_BLASTER_LR_CMD+pwmValue);
		return OrderType.LEFT;
	}
		
	private OrderType  internal_right(int strength)
	{
		String pwmValue = strength == 0 ? "0" : pwmValueFormat.format(((((double)strength)/100d)/2d)+0.5d);
		if(LOGGER.isDebugEnabled()) LOGGER.debug("internal_right strength=[{}] pwmValue=[{}]", strength, pwmValue);
		piBlasterWrite(PI_BLASTER_LR_CMD+pwmValue);
		return OrderType.RIGHT;
	}
	
	private OrderType internal_lr_stop()
	{
		piBlasterWrite(PI_BLASTER_STOP_LR_CMD);	
		return OrderType.STOP;
	}
	
	@RequestMapping(value = "/calibrate/steeringwheel", method = POST, consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
	public void calibrate() throws Exception
	{
		try 
		{
			LOGGER.info("Starting steering wheel left calibration -> steeringWheelSensorCurrentValue=[{}]", steeringWheelSensorCurrentValue);
			
			piBlasterWrite(PI_BLASTER_LR_CMD+"0");//Left
			Thread.sleep(2000);
			steeringWheelSensorMaxLeftValue = this.steeringWheelSensorCurrentValue; 
			
			LOGGER.info("Starting steering wheel right calibration -> steeringWheelSensorCurrentValue=[{}]", steeringWheelSensorCurrentValue);
			
			piBlasterWrite(PI_BLASTER_LR_CMD+"1");//Right
			Thread.sleep(2000);
			steeringWheelSensorMaxRightValue = this.steeringWheelSensorCurrentValue;
			
			steeringWheelSensorCenterValue = (steeringWheelSensorMaxRightValue-steeringWheelSensorMaxLeftValue)/2.0;
	
			LOGGER.info("Steering wheel calibration finished -> steeringWheelSensorMaxLeftValue=[{}] steeringWheelSensorMaxRightValue=[{}] steeringWheelSensorCenterValue=[{}]", steeringWheelSensorMaxLeftValue, steeringWheelSensorMaxRightValue, steeringWheelSensorCenterValue);
		} 
		catch (Exception e) 
		{
			LOGGER.error("Error calibrating steering wheel", e);
		}
		finally
		{
			internal_lr_stop();
		}
	}
	
	
	@RequestMapping(value = "/stop", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void stop()
	{
		synchronized (lastFBOrderLock) 
		{
			lastFBOrderSpeed = 0;
			lastFBOrderType = OrderType.STOP;
			lastFBOrderDate = LocalDateTime.now();
		}
	}
	
	/**
		0-60 forward + right
		60-120 forward
		120-180 forward + left
		180-240 backward + left
		240-300 backward
		300-360 backward + right
	*/
	@RequestMapping(value = "/joystick/{angle}/{strength}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void joystick(@PathVariable("angle") int angle, @PathVariable("strength") int strength)
	{
		this.throttleControl = Status.OFF;
		if(LOGGER.isDebugEnabled()) LOGGER.debug("joystick angle=[{}] strength=[{}]", angle, strength);
			
		strength = Math.min(Math.max(strength, MIN_FB_STRENGTH), MAX_FB_STRENGTH);
		
		if(strength > NEAR_MAX_FB_STRENGTH)
		{
			strength = MAX_FB_STRENGTH;
		}
		
		if(angle <= 0 && strength <= 0)
		{
			fb_stop();
		}
		else if(0 < angle && angle <= 180)
		{
			if(engineControl) 
			{
				forward(strength);
			}
		}
		else
		{
			if(engineControl) 
			{
				backward(strength);
			}
		}
		
		if(angle <= 0 && strength <= 0)
		{
			lr_stop();
		}
		else
		{
			if(steeringWheelControl) 
			{
				steeringwheel(angle <= 180 ? angle : 360 - angle);
			}
		}
	}
	
	private void fb_stop()
	{
		synchronized (lastFBOrderLock) 
		{
			lastFBOrderSpeed = 0;
			lastFBOrderDate = LocalDateTime.now();
			previousFBProcessedOrderType = internal_fb_stop();
			lastFBOrderType = OrderType.STOP;
		}
	}
	
	@RequestMapping(value = "/forward/{speed}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void forward(@PathVariable("speed") int speed)
	{
		synchronized (lastFBOrderLock) 
		{
			lastFBOrderSpeed = speed;
			lastFBOrderType = OrderType.FORDWARD;
			lastFBOrderDate = LocalDateTime.now();
		}
	}
	
	@RequestMapping(value = "/backward/{speed}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void backward(@PathVariable("speed") int speed)
	{
		synchronized (lastFBOrderLock) 
		{
			lastFBOrderSpeed = speed;
			lastFBOrderType = OrderType.BACKWARD;
			lastFBOrderDate = LocalDateTime.now();
		}
	}
		
	/**
	 * steeringWheelSensorMaxRightValue=[1653.0] 
	 * steeringWheelSensorMaxLeftValue=[0.0]
	 * steeringWheelSensorCenterValue=[826.5]
	 * @param angle
	 */	
	@RequestMapping(value = "/steeringwheel/{angle}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void steeringwheel(@PathVariable("angle") int angle)
	{
		synchronized (lastLROrderLock) 
		{
			lastLRDesiredAngle = angle;
			lastLROrderDate = LocalDateTime.now();
		}
	}
	
	private double getSteeringWheelCurrentSensorAngle()
	{
		double maxValue = (steeringWheelSensorMaxRightValue-steeringWheelSensorMaxLeftValue);

		double value = (DG180-((DG180*steeringWheelSensorCurrentValue)/maxValue));
		
		if(value < DG000)
		{
			return DG000;
		}
		
		if(value > DG180)
		{
			return DG180;
		}
		
		return value;
	}
	
	private double getEquivalentSteeringWheelSensorValue(double angle)
	{
		double maxValue = (steeringWheelSensorMaxRightValue-steeringWheelSensorMaxLeftValue);
		return Math.abs(maxValue-((maxValue*angle)/DG180));
	}
	
	private void lr_stop()
	{
		synchronized (lastLROrderLock) 
		{
			lastLROrderDate = LocalDateTime.now();
			previousLRProcessedOrderType = internal_lr_stop();
			lastLROrderType = OrderType.STOP;
		}
	}

	@RequestMapping(value = "/status/{complete}", method = GET, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public GeneralStatus status(@PathVariable("complete") boolean complete)
	{
		GeneralStatus ret = new GeneralStatus();
		
		ret.setLastFBOrderSpeed(this.lastFBOrderSpeed);
		ret.setLastFBOrderType(lastFBOrderType);
		ret.setLastFBOrderDate(lastFBOrderDate);
		
		ret.setLastLRDesiredAngle(this.lastLRDesiredAngle);
		ret.setLastLROrderType(lastLROrderType);
		ret.setLastLROrderDate(lastLROrderDate);
		
		if(complete)
		{
			ret.setLights(relayLights.isHigh() ? Status.ON : Status.OFF);
			ret.setFans(relayFans.isHigh() ? Status.ON : Status.OFF);
			ret.setEngineControl(engineControl ? Status.ON : Status.OFF);
			ret.setSteeringWheelControl(steeringWheelControl ? Status.ON : Status.OFF);
			
			ret.setMobileControl((relayMobileCrlMainGround.isHigh() &&
								relayMobileCrlMainPower.isHigh() &&
								relayMobileCrlMotor1a.isHigh() &&
								relayMobileCrlMotor1b.isHigh() &&
								relayMobileCrlMotor2a.isHigh() &&
								relayMobileCrlMotor2b.isHigh()) ? Status.ON : Status.OFF);
		}

		return ret;
	}
	
	@RequestMapping(value = "/mobilecontrol/{status}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void mobilecontrol(@PathVariable("status") Status status)
	{
		switch (status) 
		{
			case ON:
			{	
				relayMobileCrlMainGround.high();
				relayMobileCrlMainPower.high();
				relayMobileCrlMotor1a.high();
				relayMobileCrlMotor1b.high();
				relayMobileCrlMotor2a.high();
				relayMobileCrlMotor2b.high();
				break;
			}
			case OFF:
			default:
			{
				relayMobileCrlMainGround.low();
				relayMobileCrlMainPower.low();
				relayMobileCrlMotor1a.low();
				relayMobileCrlMotor1b.low();
				relayMobileCrlMotor2a.low();
				relayMobileCrlMotor2b.low();
				break;
			}
		}
	}
	
	@RequestMapping(value = "/lights/{status}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void lights(@PathVariable("status") Status status)
	{			
		switch (status) 
		{
			case ON:
			{	
				relayLights.high();
				break;
			}
			case OFF:
			default:
			{
				relayLights.low();
				break;
			}
		}
	}
	
	@RequestMapping(value = "/throttlecontrol/{status}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void throttlecontrol(@PathVariable("status") Status status)
	{			
		this.throttleControl = status;
	}

	@RequestMapping(value = "/fans/{status}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void fans(@PathVariable("status") Status status)
	{
		switch (status) 
		{
			case ON:
			{	
				relayFans.high();
				break;
			}
			case OFF:
			default:
			{
				relayFans.low();
				break;
			}
		}
	}
	
	@RequestMapping(value = "/steeringwheelcontrol/{status}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void lrstatus(@PathVariable("status") Status status)
	{
		switch (status) 
		{
			case ON:
			{	
				steeringWheelControl = true;
				break;
			}
			case OFF:
			default:
			{
				steeringWheelControl = false;
				break;
			}
		}
	}
	
	@RequestMapping(value = "/enginecontrol/{status}", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public void fbstatus(@PathVariable("status") Status status)
	{
		switch (status) 
		{
			case ON:
			{	
				engineControl = true;
				break;
			}
			case OFF:
			default:
			{
				engineControl = false;
				break;
			}
		}
	}
	
	@RequestMapping(value = "/logs", method = GET, consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
	public String logs() throws UnsupportedEncodingException, IOException
	{
		return new String(Files.readAllBytes(Paths.get("/home/pi/bcarserver/", "bcarserver.log")), "UTF-8");
	}
	
	@RequestMapping(value = "/shutdown", method = POST, consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
	public void shutdown() throws Exception
	{
		new SystemControl().shutdown();
	}
	
	@RequestMapping(value = "/reboot", method = POST, consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
	public void reboot() throws Exception
	{
		new SystemControl().reboot();
	}

	@RequestMapping(value = "/deploy", method = POST)
	public void deploy(@RequestParam("file") MultipartFile file)  throws Exception {
		FileUtils.writeByteArrayToFile(new File(this.jarPath), file.getBytes());
		this.reboot();
	}
}

