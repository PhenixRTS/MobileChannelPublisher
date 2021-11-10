/**
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All Rights Reserved.
 */

import React from 'react';
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  TouchableOpacityProps,
} from 'react-native';

export const CustomButton = ({title, onPress, disabled, ...rest}: TouchableOpacityProps & {title: string}) => (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled}
      style={styles.root}
      {...rest}
    >
      <Text style={{...styles.text, opacity: disabled ? 0.5 : 1}}>{title}</Text>
    </TouchableOpacity>
  );

const styles = StyleSheet.create({
  root: {
    flex: 1,
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 8,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 4
  },
  text: {
    fontWeight: 'bold',
    color: 'black'
  }
})