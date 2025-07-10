import { IsString, MinLength } from 'class-validator';

export class UserRegisterDto {
  @IsString()
  name: string;

  @IsString()
  @MinLength(6)
  password: string;
}
