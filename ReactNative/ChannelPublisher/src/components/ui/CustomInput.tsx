/**
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All Rights Reserved.
 */

import React from 'react';
import {
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  TouchableOpacity,
  View,
} from 'react-native';

import {CloseIcon} from '../../assets/icons';

interface CustomInputProps extends TextInputProps {
  label?: string
  onClear: () => void
}

export const CustomInput = ({label, value, placeholder, keyboardType, onChangeText, onClear, ...rest}: CustomInputProps) => (
  <View style={styles.root}>
    <TextInput
      value={value}
      placeholder={placeholder || 'Enter'}
      keyboardType={keyboardType}
      onChangeText={onChangeText}
      style={styles.input}
      placeholderTextColor="grey"
      {...rest}
    />
    {label && (
      <View style={styles.labelWrapper}>
        <Text style={styles.label}>{label}</Text>
      </View>
    )}
    <View style={styles.clearWrapper}>
      <TouchableOpacity onPress={onClear} style={styles.clearBtn}>
        <CloseIcon width={20} height={20}/>
      </TouchableOpacity>
    </View>
  </View>
);

const styles = StyleSheet.create({
  root: {
    flex: 1,
    position: 'relative',
  },
  input: {
    height: 40,
    borderWidth: 1,
    padding: 8,
    paddingRight: 32,
    color: 'black',
  },
  clearWrapper: {
    position: 'absolute',
    width: 32,
    height: '100%',
    right: 0,
    justifyContent: 'center',
    alignItems: 'center'
  },
  labelWrapper: {
    position: 'absolute',
    left: 4,
    top: 0,
  },
  label: {
    color: 'blue',
    fontSize: 10,
  },
  clearBtn: {
    width: 20,
    height: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
})