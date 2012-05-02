package scale.common;

import java.util.Locale;

/** 
 * This provides the basis for messages issued by the Scale compiler.
 * <p>
 * $Id: Msg.java,v 1.41 2007-10-04 19:53:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Additional classes should be derived from this class for each
 * natural language that is supported.  These additional classes will
 * contain the text for each message in the particular natural
 * language.  The derived classes must be named using "Msg" catenated
 * with the string returned by the
 * <code>java.util.Locale.getDisplayLanguage()</code> method.
 * <p>
 * The messages are specified by an integer.  A message may contain
 * the string <code>%s</code>.  This string will be replaced by any
 * additional text supplied when the message is displayed.
 * <p>
 * Additional message should be added to the end and given the next
 * higher number.  If a particular derived class is not updated, the
 * text "??" will be displayed.
 * <p>
 * If a message is no longer in use, replace it's text by "unused nn" and
 * replace the field name by MSG_unused_nn where nn is the value of
 * the field.  There fields can then be re-used.
 * <p>
 * All messages are displayed to <code>stderr</code>.  There is an
 * example of reporting a warning in the code for this class.
 */
public abstract class Msg
{
  public static final int MSG_alignof_is_non_standard                         =  0;
  public static final int MSG_Array_of_functions_is_not_allowed               =  1;
  public static final int MSG_Assignment_of_integer_to_pointer_without_a_cast =  2;
  public static final int MSG_Broken_pipe_s                                   =  3;
  public static final int MSG_Configuration_file_s_not_found                  =  4;
  public static final int MSG_Deprecated_s                                    =  5;
  public static final int MSG_Dereference_of_non_address                      =  6;
  public static final int MSG_Enum_s_already_defined                          =  7;
  public static final int MSG_Error_loading_flag_file_s                       =  8;
  public static final int MSG_Errors_in_compilation                           =  9;
  public static final int MSG_Field_s_already_defined                         = 10;
  public static final int MSG_Field_s_not_found                               = 11;
  public static final int MSG_Function_s_definition_does_not_match_prototype  = 12;
  public static final int MSG_Function_s_is_already_defined                   = 13;
  public static final int MSG_Ignored_s                                       = 14;
  public static final int MSG_Improper_call_to_s                              = 15;
  public static final int MSG_Improper_comment_termination                    = 16;
  public static final int MSG_Improper_string_termination                     = 17;
  public static final int MSG_Invalid_initializer                             = 18;
  public static final int MSG_Invalid_procedure_type                          = 19;
  public static final int MSG_Incompatible_function_argument_for_s            = 20;
  public static final int MSG_Incorrect_type_specification                    = 21;
  public static final int MSG_Invalid_dimension                               = 22;
  public static final int MSG_Invalid_expression                              = 23;
  public static final int MSG_Invalid_function_call                           = 24;
  public static final int MSG_Invalid_lvalue                                  = 25;
  public static final int MSG_Invalid_or_missing_type                         = 26;
  public static final int MSG_Invalid_parameter_s                             = 27;
  public static final int MSG_Invalid_storage_class                           = 28;
  public static final int MSG_Invalid_subscript_expression                    = 29;
  public static final int MSG_Invalid_type                                    = 30;
  public static final int MSG_Junk_after_directive                            = 31;
  public static final int MSG_K_R_function_definition_s                       = 32;
  public static final int MSG_Label_s_defined_twice                           = 33;
  public static final int MSG_Language_s_not_supported_using_default          = 34;
  public static final int MSG_Machine_s_not_found                             = 35;
  public static final int MSG_Macro_redefined_s                               = 36;
  public static final int MSG_Missing_return_statement_s                      = 37;
  public static final int MSG_More_than_one_source_file_specified             = 38;
  public static final int MSG_No_source_file_specified                        = 39;
  public static final int MSG_Not_a_constant                                  = 40;
  public static final int MSG_Not_a_struct                                    = 41;
  public static final int MSG_Not_a_type_s                                    = 42;
  public static final int MSG_Not_defined_s                                   = 43;
  public static final int MSG_Not_implemented_s                               = 44;
  public static final int MSG_Operands_to_s_not_compatible                    = 45;
  public static final int MSG_Operands_to_s_not_integer_types                 = 46;
  public static final int MSG_Parameter_s_already_defined                     = 47;
  public static final int MSG_Profile_CFG_characteristic_value_differs_for_s  = 48;
  public static final int MSG_Routine_s_can_not_be_optimized                  = 49;
  public static final int MSG_s                                               = 50;
  public static final int MSG_Scale_compiler_version_s                        = 51;
  public static final int MSG_s_contains_gotos                                = 52;
  public static final int MSG_s_ignored                                       = 53;
  public static final int MSG_Storage_class_specified_twice                   = 54;
  public static final int MSG_Symbol_s_already_defined                        = 55;
  public static final int MSG_There_were_s_classes_read                       = 56;
  public static final int MSG_Too_many_subscripts                             = 57;
  public static final int MSG_unused_58                                       = 58;
  public static final int MSG_Type_s_is_already_defined                       = 59;
  public static final int MSG_Unable_to_open_window_s                         = 60;
  public static final int MSG_Unable_to_send_to_s                             = 61;
  public static final int MSG_Unknown_conversion                              = 62;
  public static final int MSG_Unrecognized_declaration_attribute_s            = 63;
  public static final int MSG_Unrecognized_type_attribute_s                   = 64;
  public static final int MSG_Unrecognized_visulaizer_response_s              = 65;
  public static final int MSG_Variable_s_already_defined                      = 66;
  public static final int MSG_Debug_level_is_s                                = 67;
  public static final int MSG_Stat_level_is_s                                 = 68;
  public static final int MSG_Node_report_level_is_s                          = 69;
  public static final int MSG_Note_report_level_is_s                          = 70;
  public static final int MSG_Report_name_is_s                                = 71;
  public static final int MSG_Invalid_CFG_before_optimization_s               = 72;
  public static final int MSG_Conflicting_parameters_choose_one_of_s          = 73;
  public static final int MSG_SH_can_not_be_selected_with_0_categories        = 74;
  public static final int MSG_Optimization_s_not_performed_for_s              = 75;
  public static final int MSG_Separate_compilation                            = 76;
  public static final int MSG_Multi_compilation                               = 77;
  public static final int MSG_Target_architecture_s                           = 78;
  public static final int MSG_Host_architecture_s                             = 79;
  public static final int MSG_User_specified_annotations_added                = 80;
  public static final int MSG_Inlining_level_s                                = 81;
  public static final int MSG_No_builtins                                     = 82;
  public static final int MSG_Assume_dummy_aliases                            = 83;
  public static final int MSG_Unsafe_optimizations_allowed                    = 84;
  public static final int MSG_Floating_point_reordering_allowed               = 85;
  public static final int MSG_Signed_integer_overflows_might_not_wrap         = 86;
  public static final int MSG_All_integer_overflows_might_not_wrap            = 87;
  public static final int MSG_Data_dependence_testing_s                       = 88;
  public static final int MSG_Hyperblock_policy_backend                       = 89;
  public static final int MSG_unused_90                                       = 90;
  public static final int MSG_unused_91                                       = 91;
  public static final int MSG_Optimizations_s                                 = 92;
  public static final int MSG_Unknown_optimization_s                          = 93;
  public static final int MSG_Loop_unrolling_factor_s                         = 94;
  public static final int MSG_Performing_s                                    = 95;
  public static final int MSG_No_alias_analysis                               = 96;
  public static final int MSG_Simple_intra_procedural_alias_analysis          = 97;
  public static final int MSG_Steensgard_intra_procedural_alias_analyses      = 98;
  public static final int MSG_Shapiro_intra_procedural_alias_analyses         = 99;
  public static final int MSG_Simple_inter_procedural_alias_analysis          = 100;
  public static final int MSG_Steensgard_inter_procedural_alias_analysis      = 101;
  public static final int MSG_Shapiro_inter_procedural_alias_analysis         = 102;
  public static final int MSG_Parameter_s_requires_a_value                    = 103;
  public static final int MSG_Required_parameter_s_not_specified              = 104;
  public static final int MSG_Usage_java_s                                    = 105;
  public static final int MSG_Default_is                                      = 106;
  public static final int MSG_s_returned_status_s                             = 107;
  public static final int MSG_Include_file_s_not_found                        = 108;
  public static final int MSG_Endif_without_if                                = 109;
  public static final int MSG_Too_many_closing_parens                         = 110;
  public static final int MSG_Too_few_closing_parens                          = 111;
  public static final int MSG_Not_an_integer_constant                         = 112;
  public static final int MSG_Not_a_macro_call                                = 113;
  public static final int MSG_Too_many_macro_arguments                        = 114;
  public static final int MSG_Too_few_macro_arguments                         = 115;
  public static final int MSG_Invalid_macro_definition                        = 116;
  public static final int MSG_Pre_defined_macros_can_not_be_redefined         = 117;
  public static final int MSG_Pre_defined_macros_can_not_be_undefined         = 118;
  public static final int MSG_Unknown_file_extension_s                        = 119;
  public static final int MSG_Failed_to_compile_s                             = 120;
  public static final int MSG_Failed_to_generate_IL_file_for_multiple_files   = 121;
  public static final int MSG_Failed_to_generate_IL_file_for_s                = 122;
  public static final int MSG_Source_file_not_found_s                         = 123;
  public static final int MSG_Unknown_file_type_s                             = 124;
  public static final int MSG_Using_profiling_information                     = 125;
  public static final int MSG_Invalid_statement                               = 126;
  public static final int MSG_Expecting_s_found_s                             = 127;
  public static final int MSG_Too_many_initializers                           = 128;
  public static final int MSG_Generating_s                                    = 129;
  public static final int MSG_Assembling_s                                    = 130;
  public static final int MSG_Clef                                            = 131;
  public static final int MSG_Scribble_s                                      = 132;
  public static final int MSG_Optimizing_s                                    = 133;
  public static final int MSG_Converting_multiple_files_to_Scribble           = 134;
  public static final int MSG_Converting_to_Scribble_s                        = 135;
  public static final int MSG_Start                                           = 136;
  public static final int MSG_End                                             = 137;
  public static final int MSG_Use_Profile_Info_Start                          = 138;
  public static final int MSG_Use_Profile_Info_End                            = 139;
  public static final int MSG_Inlining_Start                                  = 140;
  public static final int MSG_Inlining_End                                    = 141;
  public static final int MSG_Alias_Analysis_Start                            = 142;
  public static final int MSG_Alias_Analysis_End                              = 143;
  public static final int MSG_Main_already_defined_in_s                       = 144;
  public static final int MSG_s_is_not_a_variable                             = 145;
  public static final int MSG_s_is_not_an_integer_variable                    = 146;
  public static final int MSG_Invalid_goto_statement                          = 147;
  public static final int MSG_Invalid_statement_label                         = 148;
  public static final int MSG_Unit_not_specified                              = 149;
  public static final int MSG_Not_an_integer_value                            = 150;
  public static final int MSG_Not_a_scalar_value                              = 151;
  public static final int MSG_Not_a_CHARACTER_value                           = 152;
  public static final int MSG_Not_a_LOGICAL_value                             = 153;
  public static final int MSG_Missing_END_statement                           = 154;
  public static final int MSG_Improper_continuation_statement                 = 155;
  public static final int MSG_Concatenation_of_unknown_length_not_allowed     = 156;
  public static final int MSG_Call_does_not_match_routine_definition          = 157;
  public static final int MSG_Unknown_intrinsic_function_s                    = 158;
  public static final int MSG_Invalid_KIND_selector                           = 159;
  public static final int MSG_Invalid_FORMAT_specifier                        = 160;
  public static final int MSG_Invalid_CFG_after_optimization_s                = 161;
  public static final int MSG_Elapsed_time_s                                  = 162;
  public static final int MSG_Parser_s_not_found                              = 163;
  public static final int MSG_The_shapes_of_the_array_expressions_do_not_conform = 164;
  public static final int MSG_Use_of_Os_with_g_is_not_recommended             = 165;
  public static final int MSG_Pragma_not_processed_s                          = 166;
  public static final int MSG_Invalid_type_operand_to_s                       = 167;
  public static final int MSG_Unable_to_instruction_schedule_s                = 168;
  public static final int MSG_Suspended_press_enter                           = 169;
  public static final int MSG_Invalid_profile_data_s                          = 170;
  // Insert new messages here. Change value of lastMsg.
  private static final int lastMsg                                            = 171;

  public static final int HLP_none    = 0;
  public static final int HLP_G       = 1;
  public static final int HLP_daVinci = 2;
  public static final int HLP_unused_3 = 3;
  public static final int HLP_d       = 4;
  public static final int HLP_dd      = 5;
  public static final int HLP_files   = 6;
  public static final int HLP_I       = 7;
  public static final int HLP_Is      = 8;
  public static final int HLP_A       = 9;
  public static final int HLP_D       = 10;
  public static final int HLP_U       = 11;
  public static final int HLP_t       = 12;
  public static final int HLP_f       = 13;
  public static final int HLP_inl     = 14;
  public static final int HLP_inls    = 15;
  public static final int HLP_unused_16 = 16;
  public static final int HLP_pi      = 17;
  public static final int HLP_pp      = 18;
  public static final int HLP_cmi     = 19;
  public static final int HLP_M       = 20;
  public static final int HLP_unused_21 = 21;
  public static final int HLP_cat     = 22;
  public static final int HLP_stat    = 23;
  public static final int HLP_cdd     = 24;
  public static final int HLP_san     = 25;
  public static final int HLP_E       = 26;
  public static final int HLP_L       = 27;
  public static final int HLP_o       = 28;
  public static final int HLP_S       = 29;
  public static final int HLP_oa      = 30;
  public static final int HLP_oc      = 31;
  public static final int HLP_ccb     = 32;
  public static final int HLP_cca     = 33;
  public static final int HLP_cgb     = 34;
  public static final int HLP_cga     = 35;
  public static final int HLP_dcg     = 36;
  public static final int HLP_c89     = 37;
  public static final int HLP_c99     = 38;
  public static final int HLP_gcc     = 39;
  public static final int HLP_ckr     = 40;
  public static final int HLP_ansi    = 41;
  public static final int HLP_unsafe  = 42;
  public static final int HLP_scb     = 43;
  public static final int HLP_sca     = 44;
  public static final int HLP_sgb     = 45;
  public static final int HLP_sga     = 46;
  public static final int HLP_version = 47;
  public static final int HLP_snap    = 48;
  public static final int HLP_gdb     = 49;
  public static final int HLP_naln    = 50;
  public static final int HLP_is      = 51;
  public static final int HLP_bi      = 52;
  public static final int HLP_ph      = 53;
  public static final int HLP_unused_54 = 54;
  public static final int HLP_np      = 55;
  public static final int HLP_quiet   = 56;
  public static final int HLP_hda     = 57;
  public static final int HLP_fpr     = 58;
  public static final int HLP_hb      = 59;
  public static final int HLP_wrap    = 60;
  public static final int HLP_arch    = 61;
  public static final int HLP_cc      = 62;
  public static final int HLP_asm     = 63;
  public static final int HLP_unused_64 = 64;
  public static final int HLP_unused_65 = 65;
  public static final int HLP_dir     = 66;
  public static final int HLP_r       = 67;
  public static final int HLP_for     = 68;
  public static final int HLP_ff      = 69;
  public static final int HLP_pg      = 70;
  public static final int HLP_AA      = 71;
  public static final int HLP_O       = 72;
  public static final int HLP_help    = 73;
  public static final int HLP_vcc     = 74;
  public static final int HLP_ccc     = 75;
  public static final int HLP_unused_76 = 76;
  public static final int HLP_occ     = 77;
  public static final int HLP_Occ     = 78;
  public static final int HLP_phelp   = 79;
  public static final int HLP_w       = 80;
  public static final int HLP_vcg     = 81;
  public static final int HLP_icf     = 82;
  public static final int HLP_sc      = 83;
  public static final int HLP_sf      = 84;
  public static final int HLP_dm      = 85;
  public static final int HLP_ih      = 86;
  public static final int HLP_phb     = 87;
  public static final int HLP_nw      = 88;
  public static final int HLP_suspend = 89;
  // Insert new help messages here. Change value of lastHlp.
  private static final int lastHlp    = 90;

  /**
   * If <code>true</code>, do not print warning messages.
   */
  public static boolean ignoreAllWarnings = false;
  /**
   * If <code>true</code>, print informational messages.
   */
  public static boolean reportInfo = true;

  private static String[] messages;
  private static String[] helpMessages;
  private static String   error         = "Error";
  private static String   warning       = "Warning";
  private static String   info          = "Info";
  private static BitVect  ignoreWarning = new BitVect();

  protected Msg()
  {
  }

  protected abstract String[] getMessages();
  protected abstract String[] getHelpMessages();
  protected abstract String   getErrorString();
  protected abstract String   getWarningString();
  protected abstract String   getInfoString();

  public static final String getMessage(int msgIndex)
  {
    if ((msgIndex >= 0) && (msgIndex < messages.length))
      return messages[msgIndex];
    return "??";
  }

  public static final String getHelpMessage(int msgIndex)
  {
    if ((msgIndex >= 0) && (msgIndex < helpMessages.length))
      return helpMessages[msgIndex];
    return "??";
  }

  /**
   * Return the specified string with the first occurrence of
   * <code>%s</code> replaced by the specified text.
   */
  public static String insertText(String str, String text)
  {
    if (text == null)
      text = "null";

    int p = str.indexOf("%s");
    if (p < 0)
      return str;

    return str.substring(0, p) + text + str.substring(p + 2);
  }

  /**
   * Return the specified string with the first and second occurrences
   * of <code>%s</code> replaced by the specified text.
   */
  public static String insertText(String str, String text1, String text2)
  {
    return insertText(insertText(str, text1), text2);
  }

  /**
   * Return the specified string with the first occurrence of
   * <code>%s</code> replaced by the specified text.
   */
  public static String insertText(int errno, String text)
  {
    if (text == null)
      text = "null";

    String str = getMessage(errno);

    int p = str.indexOf("%s");
    if (p < 0)
      return str;

    return str.substring(0, p) + text + str.substring(p + 2);
  }

  /**
   * Return the specified string with the first and second occurrences
   * of <code>%s</code> replaced by the specified text.
   */
  public static String insertText(int errno, String text1, String text2)
  {
    return insertText(insertText(errno, text1), text2);
  }

  /**
   * Initialize the message system before it is used.
   * @param language is the language to use or <code>null</code> for
   * the default for this locale
   */
  public static void setup(String language)
  {
    if (language == null) {
      Locale locale = Locale.getDefault();
      language = locale.getDisplayLanguage();
    }

    String  nameM = "scale.common.Msg" + language;
    Msg     msg   = null;
    boolean found = false;
    try {
      Class[]  argNamesm = new Class[0];
      Object[] argsm     = new Object[0];
      Class<?> opClass   = Class.forName(nameM);
      java.lang.reflect.Constructor cnstr = opClass.getDeclaredConstructor(argNamesm);
      msg = (Msg) cnstr.newInstance(argsm);
      found = true;
    } catch (java.lang.Exception ex) {
    }

    if (!found)
      msg = new MsgEnglish();

    messages     = msg.getMessages();
    helpMessages = msg.getHelpMessages();
    error        = msg.getErrorString();
    warning      = msg.getWarningString();
    info         = msg.getInfoString();

    // Make sure that the specific language class used have been
    // properly generated.

    assert (messages.length == lastMsg) : "messages " + messages.length;
    assert (helpMessages.length == lastHlp) : "help messages " + helpMessages.length;

    if (!found)
      reportWarning(MSG_Language_s_not_supported_using_default, null, 0, 0, language);
  }

  /**
   * Report the error message for the file at the line and column
   * specified.
   * @param errno specifies the error message
   * @param filename is the filename for the message and may be
   * <code>null</code>
   * @param lineno is the line number if greater than 0
   * @param column is the column number if greater than 0
   * @param text1 is text to insert into the message and may be
   * <code>null</code>
   * @param text2 is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportError(int    errno,
                                 String filename,
                                 int    lineno,
                                 int    column,
                                 String text1,
                                 String text2)
  {
    printPreamble(error, errno, filename, lineno, column);
    System.err.println(insertText(getMessage(errno), text1, text2));
  }

  /**
   * Report the error message for the file at the line and column
   * specified.
   * @param errno specifies the error message
   * @param filename is the filename for the message and may be
   * <code>null</code>
   * @param lineno is the line number if greater than 0
   * @param column is the column number if greater than 0
   * @param text is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportError(int    errno,
                                 String filename,
                                 int    lineno,
                                 int    column,
                                 String text)
  {
    printPreamble(error, errno, filename, lineno, column);
    System.err.println(insertText(getMessage(errno), text));
  }

  /**
   * Report the error message for the file at the line and column
   * specified.
   * @param errno specifies the error message
   * @param text is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportError(int errno, String text)
  {
    reportError(errno, null, 0, 0, text);
  }

  /**
   * Report the warning message for the file at the line and column
   * specified.
   * @param errno specifies the warning message
   * @param filename is the filename for the message and may be
   * <code>null</code>
   * @param lineno is the line number if greater than 0
   * @param column is the column number if greater than 0
   * @param text is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportWarning(int    errno,
                                   String filename,
                                   int    lineno,
                                   int    column,
                                   String text)
  {
    if (ignoreAllWarnings || ignoreWarning.get(errno))
      return;

    printPreamble(warning, errno, filename, lineno, column);
    System.err.println(insertText(getMessage(errno), text));
  }

  /**
   * Report the warning message for the file at the line and column
   * specified.
   * @param errno specifies the warning message
   * @param text is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportWarning(int    errno, String text)
  {
    if (ignoreAllWarnings || ignoreWarning.get(errno))
      return;

    printPreamble(warning, errno, null, 0, 0);
    System.err.println(insertText(getMessage(errno), text));
  }

  /**
   * Report the warning message for the file at the line and column
   * specified.
   * @param errno specifies the warning message
   * @param filename is the filename for the message and may be
   * <code>null</code>
   * @param lineno is the line number if greater than 0
   * @param column is the column number if greater than 0
   * @param text1 is text to insert into the message and may be
   * <code>null</code>
   * @param text2 is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportWarning(int    errno,
                                   String filename,
                                   int    lineno,
                                   int    column,
                                   String text1,
                                   String text2)
  {
    if (ignoreAllWarnings || ignoreWarning.get(errno))
      return;

    printPreamble(warning, errno, filename, lineno, column);
    System.err.println(insertText(getMessage(errno), text1, text2));
  }

  /**
   * Report the informational message for the file at the line and
   * column specified.
   * @param errno specifies the informational message
   * @param filename is the filename for the message and may be
   * <code>null</code>
   * @param lineno is the line number if greater than 0
   * @param column is the column number if greater than 0
   * @param text is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportInfo(int    errno,
                                String filename,
                                int    lineno,
                                int    column,
                                String text)
  {
    if (!reportInfo)
      return;

    printPreamble(info, errno, filename, lineno, column);
    System.err.println(insertText(getMessage(errno), text));
  }

  /**
   * Report the informational message for the file at the line and
   * column specified.
   * @param errno specifies the informational message
   * @param text is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportInfo(int errno, String text)
  {
    reportInfo(errno, null, 0, 0, text);
  }

  /**
   * Report the informational message for the file at the line and
   * column specified.
   * @param errno specifies the informational message
   * @param filename is the filename for the message and may be
   * <code>null</code>
   * @param lineno is the line number if greater than 0
   * @param column is the column number if greater than 0
   * @param text1 is text to insert into the message and may be
   * <code>null</code>
   * @param text2 is text to insert into the message and may be
   * <code>null</code>
   */
  public static void reportInfo(int    errno,
                                String filename,
                                int    lineno,
                                int    column,
                                String text1,
                                String text2)
  {
    if (!reportInfo)
      return;

    printPreamble(info, errno, filename, lineno, column);
    System.err.println(insertText(getMessage(errno), text1, text2));
  }

  private static void printPreamble(String type,
                                    int    errno,
                                    String filename,
                                    int    lineno,
                                    int    column)
  {
    System.err.print(type);
    System.err.print(' ');
    System.err.print(errno);
    if (filename != null) {
      System.err.print(':');
      System.err.print(filename);
      if (lineno > 0) {
        System.err.print(':');
        System.err.print(lineno);
        if (column > 0) {
          System.err.print(':');
          System.err.print(column);
        }
      }
    }
    System.err.print(": ");
  }

  /**
   * Specify to ignore a specific warning.
   */
  public static void ignoreWarning(int errno)
  {
    if ((errno >= 0) && (errno < messages.length))
      ignoreWarning.set(errno);
  }

  /**
   * Specify a range of warnings to ignore.
   */
  public static void ignoreWarning(int first, int last)
  {
    if (first < 1)
      first = 1;

    if (last >= messages.length)
      last = messages.length - 1;

    for (int i = first; i <= last; i++)
      ignoreWarning.set(i);
  }
}
