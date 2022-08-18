package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest
{

    public SecurityService securityService;

    @Mock
    public ImageService imageService;

    @Mock
    public SecurityRepository securityRepository;

    static HashSet<Sensor> sensors = new HashSet<>();

    Sensor sensor;


    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("door", SensorType.DOOR);
    }

    //If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    public void alarmArmed_sensorBecomesactivated_putSystemIntoPendingAlarmStatus ()
    {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    //If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on.
    @Test
    public void alarmArmed_sensorActivated_systemPendingAlarm__alarmStatusAlarm () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void pendingAlarm_sensorsInactive_returnAlarmState () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    public void alarmActive_changeSensorState_shouldNotAffectAlarmState () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    //If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void sensorActivated_alreadyActive_systemPendingStateChangeAlarmState
    () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    public void sensorDeactivated_alreadyInactive_noChangeAlarmState () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }
    //If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void cameraImageContainsCat_systemArmedHome_putSystemAlarmStatus () {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void cameraImageNotContainCat_changeStatusNoAlarm_sensorsNotActive
    () {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        sensor.setActive(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If the system is disarmed, set the status to no alarm.
    @Test
    public void systemDisarmed_setStatusNoAlarm () {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void systemArmed_resetAllSensorsInactive (ArmingStatus status) {
        Sensor sensorMotion = new Sensor("motion", SensorType.MOTION);
        Sensor sensorWindow = new Sensor("window", SensorType.WINDOW);
        Sensor sensorDoor = new Sensor("door", SensorType.DOOR);
        sensors.add(sensorDoor);
        sensors.add(sensorWindow);
        sensors.add(sensorMotion);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensors.forEach(sensor -> sensor.setActive(true));
        securityService.setArmingStatus(status);
        sensors.forEach(sensor -> Assertions.assertEquals(false, sensor.getActive()));
    }

    //If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void systemArmedHomeWhileCameraShowsCat_setAlarmStatusToAlarm () {
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 12
    @Test
    public void systemArmedHome_SetAlarmStatusAlarm () {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(securityService.getArmingStatus());
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

}