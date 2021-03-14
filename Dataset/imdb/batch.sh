#!/usr/bin/env bash

# Sync the dataset
./sync.sh

# Create snapshots for the following lists from 2017-10-13 to 2017-12-25.
timestamp=171013
./patch.sh actors $timestamp
./patch.sh actresses $timestamp
./patch.sh countries $timestamp
./patch.sh directors $timestamp
./patch.sh distributors $timestamp
./patch.sh genres $timestamp
./patch.sh language $timestamp
./patch.sh movies $timestamp
./patch.sh ratings $timestamp
