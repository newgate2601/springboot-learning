package com.example.learning.inboxoutbox.common.payload;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomDto {
  private Long id;
  private String name;
  private List<Custom2Dto> list;
  private Custom3Dto custom;
}
