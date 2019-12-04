# Fall-Alerter

A Fall Alerter application is created using built-in acceleration sensor of the phone. In case of a fall, user may lose consciousness and may require external help, so the application alerts caregivers via SMS with the GPS data of the fall location. The UI contains a graph showing acceleration data of the phone and an emergency contact list. If it detects a fall, a 10-second countdown begins and at the end, emergency contact list will be alerted. User can turn the alert off if it is a false alarm or does not require help despite the fall. A demo of the app is given below.
<br><br>

<p align="center">
  <img align="left" src="https://github.com/barkinak/Fall-Detector/blob/master/content/App%20demo.gif" width="360" title="App GIF">
</p>

The graph below shows the acceleration variation during a fall event. Each fall event has a distinctive characteristic. While the phone is falling, the accelerometer value starts to decrease. When the free fall event is over and the impact to ground occurs, we will observe a sudden spike. Tests have shown that similar variations are observed in fall events. 
<br><br>

<p align="center">
  <img align="center" src="https://github.com/barkinak/Fall-Detector/blob/master/content/Acceleration%20variation.png" width="360">
</p>

Sensor API uses a three-dimensional coordinate system. The accelerometer value is calculated by taking the squares of each dimension, summing them up and taking the square root. By doing so, we obtain the magnitude of the vector direction. When the phone is at rest, acceleration value is around 9.8 m/s^2.
<br><br><br>

### Fall Detection Algorithm
Discrete Wavelet Transform is used to extract feature vectors to be compared with input acceleration data. A mother wavelet is chosen as Meyer wavelet with 32 sample points, from which two filters are obtained. These filters are used on input signal so that we have two different signals, namely approximation and detail coefficients. Then, they are downsampled by a factor of 2, which gives first scale outputs. Experimentally determined threshold is used to extract feature vectors for different events such as sitting, walking and falling. Threshold value is 5 in this case, because detail coefficients for falling event have components higher than 5 unlike sitting and walking event. If detail coefficient for a signal is bigger than 5, value of feature vector coefficient is 1, otherwise 0. Due to this, feature vectors for walking and sitting events are all zero. Therefore, input signal is simply compared with zero vector and feature vector obtained from a fall event. Using MATLAB, these vectors and filter coefficients were calculated and are used as constant variables in the code. The signal is taken in 256 point windows.

### Disclaimer
It should be noted that this application does not guarantee fall detections with complete accuracy and it also does not prevent falls. It may generate false alarms or may fail to catch a real fall in some circumstances. Use it at your own discretion.
