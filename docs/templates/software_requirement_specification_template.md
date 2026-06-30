**&lt;Project Name&gt;**

**System-Wide Requirements Specification**

_Usage note: There is procedural guidance within this template that appears in a style named InfoBlue. This style has a hidden font attribute allowing you to toggle whether it is visible or hidden in this template. Use the Word menu Tools🡪Options🡪View🡪Hidden Text checkbox to toggle this setting. A similar option exists for printing Tools🡪Options🡪Print._

# **Introduction**

# **System-Wide Functional Requirements**

_\[Statement of system-wide functional requirements, not expressed as use cases. Examples include auditing, authentication, printing, reporting.\]_

# **System Qualities**

_\[Qualities represent the URPS in FURPS+ classification of supporting requirements.\]_

## **Usability**

_\[Describe requirements for qualities such as easy of use, easy of learning, usability standards and localization.\]_

## **Reliability**

_\[Reliability includes the product and/or system's ability to keep running under stress and adverse conditions. Specify requirements for reliability acceptance levels, and how they will be measured and evaluated. Suggested topics are availability, frequency of severity of failures and recoverability.\]_

## **Performance**

_\[The performance characteristics of the system should be outlined in this section. Examples are response time, throughput, capacity and startup or shutdown times.\]_

## **Supportability**

_\[This section indicates any requirements that will enhance the supportability or maintainability of the system being built, including adaptability and upgrading, compatibility, configurability, scalability and requirements regarding system installation, level of support and maintenance.\]_

# **System Interfaces**

_\[Interface Requirements are part of the + in the FURPS+ classification of supporting requirements. Define the interfaces that must be supported by the application. It should contain adequate specificity, protocols, ports and logical addresses, and so forth, so that the software can be developed and verified against the interface requirements.\]_

## **User Interfaces**

_\[Describe the user interfaces that are to be implemented by the software. The intention of this section is to state requirements relating to the interface. Interface design may overlap the requirements gathering process.\]_

### _Look & Feel_

_\[Provide a description of the spirit of the interface. Your client may have given you particular demands such as style, colors to be used, and degree of interaction and so on. This section captures the requirements for the interface rather than the design for the interface.\]_

### _Layout and Navigation Requirements_

_\[Capture requirements on major screen areas and how they should be grouped together.\]_

### _Consistency_

_\[Consistency in the user interface enables users to predict what will happen. This section states requirements on the use of mechanisms to be employed in the user interface. This applies both within the system and with other systems and can be applied at different levels: navigation controls, screen areas sizes and shapes, placements for entering / presenting data, terminology.\]_

### _User Personalization & Customization Requirements_

_\[Requirements on content that should automatically displayed to users or available based on user attributes. Sometimes users allowed to customize the content displayed or to personalize displayed content.\]_

## **Interfaces to External Systems or Devices**

_\[Are there any external systems with which this system must interface? Are there any constraints on the nature of the interface between this system and any external system, such as the format of data passed between these systems, and any particular protocol used? Consider both provided and required interfaces.\]_

### _Software Interfaces_

_\[This section describes software interfaces to other components of the software system. These may be purchased components, components reused from another application or components being developed for subsystems outside of the scope of this SRS, but with which this software application must interact.\]_

### _Hardware Interfaces_

_\[This section defines any hardware interfaces that are to be supported by the software, including logical structure, physical addresses, expected behavior, and so on.\]_

### _Communications Interfaces_

_\[Describe any communications interfaces to other systems or devices such as local area networks, remote serial devices, and so on.\]_

# **Business Rules**

_\[Business rules are statements that define or constrain some aspect of the business. Business rules are often represented as production rules when they are meant to be directly executed by an IT System: a production rule is an independent statement of programming logic that specifies the execution of one or more actions in the case that its conditions are satisfied. Production Rules define the operation semantic for the system in a technologic independent way. They constrain the behavior expressed in system use cases._

_Organize this document on rule classes, a high level grouping of candidate or actual rules about one_ **_business concept_** _with a specific kind of_ **_logic processing_**_, example: Driver Risk Assessment Rules or Customer Validation Rules.\]_

## **&lt;Rule class name&gt;**

### _&lt;Rule name and ID&gt;_

_\[The description defines the rule. It can be made in natural language typically following a decision table or a pattern like: if \[condition-list\] then \[action-list\], example:_

_If there are at least 3 items of the same type in the customer shopping cart and each item's value is greater than \$30 then give to the customer a voucher whose value is 10% of the cheapest item.\]_

# **System Constraints**

_\[Constraints are part of the + in the FURPS+ classification of supporting requirements. Describe any design; implementation or deployment constraints on the system being built that have been mandated and must be adhered to. Examples include software implementation languages, prescribed use of developmental tools, third-party components or class libraries, platform support, resource limits and requirements on the shape, size or weight of the resulting hardware housing the system.\]_

# **System Compliance**

## **Licensing Requirements**

_\[Define any licensing enforcement requirements or other usage restriction requirements that are to be exhibited by the software.\]_

## **Legal, Copyright, and Other Notices**

_\[This section describes any necessary legal disclaimers, warranties, copyright notices, patent notice, wordmark, trademark, or logo compliance issues for the software.\]_

## **Applicable Standards**

_\[This section describes by reference any applicable standards and the specific sections of any such standards that apply to the system being described. For example, this could include legal, quality and regulatory standards, industry standards for usability, interoperability, internationalization, operating system compliance, and so forth.\]_

# **System Documentation**

_\[Describes the requirements, for on-line user documentation, help systems, help about notices, and so on. Set expectations for the documentation and to identify who will be responsible for creating it.\]_

# 9\. Traceability Table

# 10\. Prompts