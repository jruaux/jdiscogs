package org.ruaux.jdiscogs.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Range {
    private double min;
    private double max;

    public boolean accept(double value) {
        return value >= min && value <= max;
    }


}